package co.com.crediya.sqs.listener;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.sqs.listener.exception.NotAbleToHandleSQSResponseException;
import co.com.crediya.usecase.updateloanstatus.UpdateLoanStatusUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private final UpdateLoanStatusUseCase updateLoanRequest;

    @Override
    public Mono<Void> apply(Message message) {
        String body = message.body();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new NotAbleToHandleSQSResponseException("Not able to process SQS message");
        }

        Loan loan = Loan.builder()
                .id(UUID.fromString(node.get("loanId").asText()))
                .email(node.get("decision").asText())
                .build();

        return updateLoanRequest.updateLoanStatusFromLambda(loan).then();
    }
}
