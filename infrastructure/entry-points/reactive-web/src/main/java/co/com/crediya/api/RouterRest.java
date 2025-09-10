package co.com.crediya.api;

import co.com.crediya.api.config.LoanPath;
import co.com.crediya.api.openapi.LoanApiDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RouterRest {

    private final LoanPath loanPath;
    private final Handler loanHandler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return route()
                .POST(loanPath.getLoan(), loanHandler::saveLoan, LoanApiDoc::saveLoanDoc)
                .GET(loanPath.getLoan(), loanHandler::getLoan, LoanApiDoc::getLoanDoc)
                .PUT(loanPath.getLoan(), loanHandler::updateLoan, LoanApiDoc::updateLoanDoc)
                .build();

    }
}
