package co.com.crediya.usecase.requestloan;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.model.loan.gateways.LoanStatusRepository;
import co.com.crediya.model.loan.gateways.LoanTypeRepository;
import co.com.crediya.model.loan.gateways.UserGateway;
import co.com.crediya.usecase.requestloan.exception.EmailNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanStatusNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RequestLoanUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanStatusRepository loanStatusRepository;
    private final UserGateway userGateway;


    public Mono<Loan> saveLoan(Loan loan, String loanTypeName) {
        return userGateway.getEmailByIdentification(loan.getEmail())
                .switchIfEmpty(Mono.error(new EmailNotFoundException("Email no encontrado para ese documento")))
                .flatMap(email -> {
                    loan.setEmail(email);
                    return Mono.zip(
                            loanTypeRepository.getIdByName(loanTypeName)
                                    .switchIfEmpty(Mono.error(new LoanTypeNotFoundException("LoanType no encontrado: " + loanTypeName))),
                            loanStatusRepository.getIdByName("PENDING")
                                    .switchIfEmpty(Mono.error(new LoanStatusNotFoundException("LoanStatus no encontrado: PENDING")))
                    );
                })
                .map(tuple -> {
                    loan.setLoanTypeId(tuple.getT1());
                    loan.setLoanStatusId(tuple.getT2());
                    return loan;
                })
                .flatMap(loanRepository::saveLoan);
    }

}
