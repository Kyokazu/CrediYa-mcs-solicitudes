package co.com.crediya.model.loan.gateways;

import reactor.core.publisher.Mono;

public interface NotificationQueueGateway {
    Mono<Void> sendMessage(String email, String message);
}
