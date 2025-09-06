package co.com.crediya.api;

import co.com.crediya.api.config.LoanPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RouterRest {

    private final LoanPath loanPath;
    private final Handler loanHandler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return route()
                .POST(loanPath.getSaveLoan(), loanHandler::saveLoan)
                .build();

    }
}
