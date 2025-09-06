package co.com.crediya.api;

import co.com.crediya.api.dto.LoanDTO;
import co.com.crediya.usecase.requestloan.exception.EmailNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanStatusNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanTypeNotFoundException;
import co.com.crediya.api.exception.ValidationException;
import co.com.crediya.api.util.ValidatorUtil;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.UserGateway;
import co.com.crediya.usecase.requestloan.RequestLoanUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final ValidatorUtil validatorUtil;
    private final TransactionalOperator txOperator;
    private final RequestLoanUseCase requestLoanUseCase;


    public Mono<ServerResponse> saveLoan(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(LoanDTO.class)
                .flatMap(validatorUtil::validate)
                .flatMap(dto -> {
                    Loan loan = Loan.builder()
                            .email(dto.getIdentification())
                            .amount(dto.getAmount())
                            .duration(dto.getDuration())
                            .build();
                    return requestLoanUseCase.saveLoan(loan, dto.getLoanType());
                })
                .as(txOperator::transactional)
                .flatMap(savedLoan -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(savedLoan))
                .onErrorResume(ValidationException.class, e ->
                        ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "status", e.getStatus(),
                                        "error", "Errores de validación",
                                        "errors", e.getErrors(),
                                        "timestamp", LocalDateTime.now().toString()
                                )));


    }
}
