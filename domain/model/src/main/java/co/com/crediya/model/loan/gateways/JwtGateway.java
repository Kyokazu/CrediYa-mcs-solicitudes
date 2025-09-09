package co.com.crediya.model.loan.gateways;

import co.com.crediya.model.loan.UserTokenInfo;
import reactor.core.publisher.Mono;

public interface JwtGateway {
    Mono<UserTokenInfo> validateToken(String token);

}