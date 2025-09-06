package co.com.crediya.model.loan.gateways;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanTypeRepository {
    Mono<UUID> getIdByName(String name);
}
