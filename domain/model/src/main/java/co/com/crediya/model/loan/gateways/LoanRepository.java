package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.Loan;
import reactor.core.publisher.Mono;

public interface LoanRepository {

    Mono<Loan> saveLoan(Loan loan);

}
