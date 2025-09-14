package co.com.crediya.sqs.listener;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.usecase.updateloanstatus.UpdateLoanStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private final UpdateLoanStatusUseCase updateLoanRequest;

    @Override
    public Mono<Void> apply(Message message) {
        String body = message.body();
        String loanId = body.split("\"loanId\":")[1].split(",")[0].replace("\"", "").trim();
        String decision = body.split("\"decision\":")[1].split(",")[0].replace("\"", "").trim();
        Loan loan = Loan.builder()
                .id(UUID.fromString(loanId))
                .email(decision)
                .build();

        return updateLoanRequest.updateLoanStatusFromLambda(loan).then();
    }
}
