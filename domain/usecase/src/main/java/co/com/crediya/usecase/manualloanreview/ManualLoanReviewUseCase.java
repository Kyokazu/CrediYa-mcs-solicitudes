package co.com.crediya.usecase.manualloanreview;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.manualloanreview.exception.NotConsultantRoleException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class ManualLoanReviewUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanStatusRepository loanStatusRepository;
    private final JwtGateway jwtGateway;
    private final UserGateway userGateway;

    public Flux<UserLoanInfo> getLoan(int page, int size, String filter, String token) {
        return validateUserWithToken(token)
                .thenMany(Flux.defer(() -> fetchLoans(page, size, filter)
                        .flatMap(this::mapToUserLoanInfo)));
    }

    private Flux<Loan> fetchLoans(int page, int size, String filter) {
        if ("ALL".equalsIgnoreCase(filter)) {
            return loanRepository.findAllPaged(page, size);
        }
        return loanTypeRepository.getIdByName(filter)
                .flatMapMany(typeId ->
                        loanRepository.findAllPaged(page, size)
                                .filter(loan -> loan.getLoanTypeId().equals(typeId))
                )
                .switchIfEmpty(Flux.empty());
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
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid token")))
                .flatMap(userTokenInfo -> {
                    if (!"CONSULTANT".equalsIgnoreCase(userTokenInfo.getRole())) {
                        return Mono.error(new NotConsultantRoleException("Access denied: User does not have CONSULTANT role"));
                    }
                    return Mono.empty();
                });
    }

    BigDecimal calculateMonthlyPayment(BigDecimal amount, Double annualInterestRate, Long duration) {
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
}

