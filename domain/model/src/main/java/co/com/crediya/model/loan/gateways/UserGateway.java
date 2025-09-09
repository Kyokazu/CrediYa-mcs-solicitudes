package co.com.crediya.model.loan.gateways;


import co.com.crediya.model.loan.UserLoanInfo;
import reactor.core.publisher.Mono;

public interface UserGateway {
    Mono<String> getEmailByIdentification(String identification);

    Mono<UserLoanInfo> getLoanRequesterInfo(String email);

}