package co.com.crediya.model.loan.gateways;

import reactor.core.publisher.Mono;

public interface UserGateway {
    Mono<String> getEmailByIdentification(String identification);
}