package co.com.crediya.usecase.requestloan;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.UserDebtCapacity;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.requestloan.exception.*;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RequiredArgsConstructor
public class RequestLoanUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanStatusRepository loanStatusRepository;
    private final UserGateway userGateway;
    private final JwtGateway jwtGateway;
    private final AWSSQSGateway awsSqsGateway;

    public Mono<Loan> saveLoan(Loan loan, String loanTypeName, String token) {
        return getEmailFromIdentification(loan.getEmail())
                .flatMap(email ->
                        validateUserWithToken(email, token)
                                .then(getLoanTypeByNameOrFail(loanTypeName)
                                        .flatMap(loanType ->
                                                buildCompleteLoan(loan, loanType, email, "PENDING")
                                                        .flatMap(builtLoan ->
                                                                saveAndMaybeEnqueueValidation(builtLoan, loanType, email)
                                                        )
                                        )
                                )
                );
    }

    private Mono<Loan> saveAndMaybeEnqueueValidation(Loan loan, LoanType loanType, String email) {
        return loanRepository.saveLoan(loan)
                .flatMap(savedLoan -> {
                    if (Boolean.TRUE.equals(loanType.getAutomaticValidation())) {
                        return enqueueLoanAutomaticValidation(savedLoan, loanType, email)
                                .thenReturn(savedLoan);
                    }
                    return Mono.just(savedLoan);
                });
    }

    private Mono<Void> enqueueLoanAutomaticValidation(Loan loan, LoanType loanType, String email) {
        return userGateway.getLoanRequesterInfo(email)
                .flatMap(userLoanInfo -> monthlyDebt(email)
                        .map(currentMonthlyDebt -> mapToUserDebtCapacity(loan, loanType, email, userLoanInfo, currentMonthlyDebt))
                        .flatMap(awsSqsGateway::calculateDebtCapacity)
                );
    }

    private UserDebtCapacity mapToUserDebtCapacity(
            Loan loan,
            LoanType loanType,
            String email,
            UserLoanInfo userLoanInfo,
            BigDecimal currentMonthlyDebt
    ) {
        return new UserDebtCapacity(
                loan.getId(),
                email,
                loanType.getInterestRate(),
                loan.getDuration(),
                userLoanInfo.getIncome(),
                loan.getAmount(),
                currentMonthlyDebt
        );
    }

    private Mono<LoanType> getLoanTypeByNameOrFail(String name) {
        return loanTypeRepository.getLoanTypeByName(name)
                .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("LoanType not found: " + name)));
    }

    private Mono<Loan> buildCompleteLoan(Loan loan, LoanType loanType, String email, String statusName) {
        return getLoanStatusIdOrFail(statusName)
                .map(statusId -> {
                    loan.setEmail(email);
                    loan.setLoanTypeId(loanType.getId());
                    loan.setLoanStatusId(statusId);
                    return loan;
                });
    }

    private Mono<UUID> getLoanStatusIdOrFail(String statusName) {
        return loanStatusRepository.getIdByName(statusName)
                .switchIfEmpty(Mono.error(new LoanStatusNotFoundException("LoanStatus not found: " + statusName)));
    }

    private Mono<BigDecimal> monthlyDebt(String email) {
        return getLoanStatusIdOrFail("APPROVED")
                .flatMapMany(statusId -> loanRepository.findByUserEmail(email))
                .flatMap(loan -> loanTypeRepository.getLoanTypeById(loan.getLoanTypeId())
                        .map(loanType -> calculateMonthlyPayment(
                                loan.getAmount(),
                                loanType.getInterestRate(),
                                loan.getDuration()
                        ))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Mono<String> getEmailFromIdentification(String identification) {
        return userGateway.getEmailByIdentification(identification)
                .switchIfEmpty(Mono.error(new EmailNotFoundException("Email not found for the provided identification")));
    }

    private Mono<Void> validateUserWithToken(String emailFromIdentification, String token) {
        return jwtGateway.validateToken(token)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid token")))
                .flatMap(userTokenInfo -> {
                    if (!emailFromIdentification.equals(userTokenInfo.getEmail())) {
                        return Mono.error(new LoanRequesterException("The loan requester identity is different from the logger user"));
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

