package co.com.crediya.usecase.updateloanstatus;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.manualloanreview.exception.NotConsultantRoleException;
import co.com.crediya.usecase.requestloan.exception.InvalidTokenException;
import co.com.crediya.usecase.requestloan.exception.LoanStatusNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RequiredArgsConstructor
public class UpdateLoanStatusUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanStatusRepository loanStatusRepository;
    private final JwtGateway jwtGateway;
    private final UserGateway userGateway;
    private final AWSSQSGateway queueGateway;

    public Mono<Loan> updateLoanStatus(Loan loan, String token) {
        return validateUserWithToken(token)
                .then(getUpdatedLoan(loan))
                .flatMap(this::notifyAndReturnLoan);
    }

    public Mono<Void> updateLoanStatusFromLambda(Loan loan) {
        return getLoanStatusId(loan.getEmail())
                .flatMap(statusId ->
                        loanRepository.findById(loan.getId())
                                .flatMap(foundLoan -> {
                                    foundLoan.setId(loan.getId());
                                    foundLoan.setLoanStatusId(statusId);
                                    return loanRepository.saveLoan(foundLoan);
                                })
                )
                .then();
    }

    private Mono<Loan> getUpdatedLoan(Loan loan) {
        return getLoanStatusId(loan.getEmail())
                .flatMap(statusId ->
                        loanRepository.findById(loan.getId())
                                .flatMap(existingLoan -> {
                                    existingLoan.setLoanStatusId(statusId);
                                    return loanRepository.saveLoan(existingLoan);

                                })
                );
    }

    private Mono<Loan> notifyAndReturnLoan(Loan updatedLoan) {
        return mapToUserLoanInfo(updatedLoan)
                .flatMap(userLoanInfo -> {
                    String message = buildLoanStatusMessage(userLoanInfo);
                    return queueGateway.sendMessage(userLoanInfo.getEmail(), message)
                            .thenReturn(updatedLoan);
                });
    }

    private Mono<UUID> getLoanStatusId(String statusName) {
        return loanStatusRepository.getIdByName(statusName)
                .switchIfEmpty(Mono.error(new LoanStatusNotFoundException("LoanStatus not found: " + statusName)));
    }

    private Mono<UserLoanInfo> mapToUserLoanInfo(Loan loan) {
        Mono<UserLoanInfo> userInfoMono = userGateway.getLoanRequesterInfo(loan.getEmail());
        Mono<LoanType> loanTypeMono = loanTypeRepository.getLoanTypeById(loan.getLoanTypeId());
        Mono<String> loanStatusNameMono = loanStatusRepository.getNameById(loan.getLoanStatusId());

        return Mono.zip(userInfoMono, loanTypeMono, loanStatusNameMono)
                .map(tuple -> {
                    UserLoanInfo baseUserInfo = tuple.getT1();
                    LoanType loanType = tuple.getT2();
                    String loanStatusName = tuple.getT3();

                    BigDecimal monthlyPayment = calculateMonthlyPayment(
                            loan.getAmount(),
                            loanType.getInterestRate(),
                            loan.getDuration()
                    );

                    return baseUserInfo.toBuilder()
                            .amount(loan.getAmount())
                            .duration(loan.getDuration())
                            .loanType(loanType.getName())
                            .interestRate(loanType.getInterestRate())
                            .loanStatus(loanStatusName)
                            .monthlyLoanPayment(monthlyPayment)
                            .build();
                });
    }

    private Mono<Void> validateUserWithToken(String token) {
        return jwtGateway.validateToken(token)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid token")))
                .flatMap(userTokenInfo -> {
                    if (!"CONSULTANT".equalsIgnoreCase(userTokenInfo.getRole())) {
                        return Mono.error(new NotConsultantRoleException("Access denied: User does not have CONSULTANT role"));
                    }
                    return Mono.empty();
                });
    }

    public BigDecimal calculateMonthlyPayment(BigDecimal amount, Double annualInterestRate, Long duration) {
        if (annualInterestRate == null || duration == null || duration == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal monthlyRate = BigDecimal.valueOf(annualInterestRate)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal numerator = amount.multiply(monthlyRate);
        BigDecimal onePlusRatePow = (BigDecimal.ONE.add(monthlyRate)).pow(duration.intValue());
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(onePlusRatePow, 10, RoundingMode.HALF_UP));

        return denominator.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String buildLoanStatusMessage(UserLoanInfo info) {
        return String.format(
                "Your %s loan request for an amount of %s in a duration of %s months with a Montly cost of %S, has been %s.",
                info.getLoanType(),
                info.getAmount(),
                info.getDuration(),
                info.getLoanStatus(),
                info.getMonthlyLoanPayment()
        );
    }


}


