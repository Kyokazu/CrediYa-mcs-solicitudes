package co.com.crediya.consumer;

import co.com.crediya.consumer.config.AdaptersPath;
import co.com.crediya.consumer.dto.UserInfoDTO;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.gateways.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserRestConsumer implements UserGateway {

    private final WebClient usersWebClient;
    private final AdaptersPath adaptersPath;


    @Override
    public Mono<String> getEmailByIdentification(String identification) {
        return usersWebClient.get()
                .uri(adaptersPath.getUserByIdentification(), identification)
                .retrieve()
                .bodyToMono(UserInfoDTO.class)
                .map(UserInfoDTO::getEmail);
    }

    @Override
    public Mono<UserLoanInfo> getLoanRequesterInfo(String email) {
        return usersWebClient.get()
                .uri(adaptersPath.getUserByEmail(), email)
                .retrieve()
                .bodyToMono(UserInfoDTO.class)
                .map(dto -> UserLoanInfo.builder()
                        .userId(dto.getId())
                        .email(dto.getEmail())
                        .name(dto.getName())
                        .income(dto.getIncome())
                        .build()
                );
    }




/*
    // these methods are an example that illustrates the implementation of WebClient.
    // You should use the methods that you implement from the Gateway from the domain.
   // @CircuitBreaker(name = "testGet" /*, fallbackMethod = "testGetOk")
    public Mono<ObjectResponse> testGet() {
        return client
                .get()
                .retrieve()
                .bodyToMono(ObjectResponse.class);
    }

// Possible fallback method
//    public Mono<String> testGetOk(Exception ignored) {
//        return client
//                .get() // TODO: change for another endpoint or destination
//                .retrieve()
//                .bodyToMono(String.class);
//    }

    @CircuitBreaker(name = "testPost")
    public Mono<ObjectResponse> testPost() {
        ObjectRequest request = ObjectRequest.builder()
                .val1("exampleval1")
                .val2("exampleval2")
                .build();
        return client
                .post()
                .body(Mono.just(request), ObjectRequest.class)
                .retrieve()
                .bodyToMono(ObjectResponse.class);
    }
*/

}
