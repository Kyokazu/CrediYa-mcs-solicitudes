package co.com.crediya.model.loan.gateways;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanStatusRepository {
    Mono<UUID> getIdByName(String name);
    Mono<String>getNameById(UUID id);
}
