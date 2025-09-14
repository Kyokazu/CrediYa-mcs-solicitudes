package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.LoanType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanTypeRepository {
    Mono<LoanType> getLoanTypeById(UUID id);

    Mono<LoanType> getLoanTypeByName(String name);
}
