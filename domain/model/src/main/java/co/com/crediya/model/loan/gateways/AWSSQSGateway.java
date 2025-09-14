package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.UserDebtCapacity;
import reactor.core.publisher.Mono;

public interface AWSSQSGateway {
    Mono<Void> sendMessage(String email, String message);

    Mono<Void> calculateDebtCapacity(UserDebtCapacity userDebtCapacity);
}
