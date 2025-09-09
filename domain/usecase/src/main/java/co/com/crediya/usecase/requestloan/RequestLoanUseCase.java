package co.com.crediya.usecase.requestloan;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.requestloan.exception.*;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.UUID;

@RequiredArgsConstructor
public class RequestLoanUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanStatusRepository loanStatusRepository;
    private final UserGateway userGateway;
    private final JwtGateway jwtGateway;


    public Mono<Loan> saveLoan(Loan loan, String loanTypeName, String token) {
        return getEmailFromIdentification(loan.getEmail())
                .flatMap(emailFromIdentification ->
                        validateUserWithToken(emailFromIdentification, token)
                                .then(buildCompleteLoan(loan, loanTypeName, emailFromIdentification))
                )
                .flatMap(loanRepository::saveLoan);
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

    private Mono<Loan> buildCompleteLoan(Loan loan, String loanTypeName, String email) {
        return Mono.zip(getLoanTypeId(loanTypeName), getLoanStatusId())
                .map(tuple -> buildLoan(loan, email, tuple));
    }

    private Mono<UUID> getLoanTypeId(String loanTypeName) {
        return loanTypeRepository.getIdByName(loanTypeName)
                .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("LoanType not found: " + loanTypeName)));
    }

    private Mono<UUID> getLoanStatusId() {
        return loanStatusRepository.getIdByName("PENDING")
                .switchIfEmpty(Mono.error(new LoanStatusNotFoundException("LoanStatus not found: PENDING")));
    }

    private Loan buildLoan(Loan loan, String email, Tuple2<UUID, UUID> tuple) {
        loan.setEmail(email);
        loan.setLoanTypeId(tuple.getT1());
        loan.setLoanStatusId(tuple.getT2());
        return loan;
    }


}
