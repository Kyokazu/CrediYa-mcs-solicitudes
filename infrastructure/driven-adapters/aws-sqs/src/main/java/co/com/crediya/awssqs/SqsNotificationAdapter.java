package co.com.crediya.awssqs;

import co.com.crediya.model.loan.UserDebtCapacity;
import co.com.crediya.model.loan.gateways.AWSSQSGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;


@Service
@RequiredArgsConstructor
public class SqsNotificationAdapter implements AWSSQSGateway {

    private final SqsAsyncClient sqsAsyncClient;

    @Value("${aws.sqs.notification-queue-url}")
    private String notificationQueueUrl;
    @Value("${aws.sqs.debt-capacity-queue-url}")
    private String debtCapacityQueueUrl;

    @Override
    public Mono<Void> sendMessage(String email, String message) {
        String payload = String.format("{\"email\":\"%s\", \"body\":\"%s\"}", email, message);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(notificationQueueUrl)
                .messageBody(payload)
                .build();

        return Mono.fromFuture(() -> sqsAsyncClient.sendMessage(request)).then();
    }

    @Override
    public Mono<Void> calculateDebtCapacity(UserDebtCapacity userDebtCapacity) {
        String payload = String.format(
                "{" +"\"id\": \"%s\", " +
                        "\"email\": \"%s\", " +
                        "\"interestRate\": %s, " +
                        "\"duration\": %s, " +
                        "\"income\": %s, " +
                        "\"amount\": %s, " +
                        "\"monthlyDebt\": %s" +
                        "}",
                userDebtCapacity.id(),
                userDebtCapacity.email(),
                userDebtCapacity.interestRate(),
                userDebtCapacity.duration(),
                userDebtCapacity.income(),
                userDebtCapacity.amount(),
                userDebtCapacity.monthlyDebt()
        );

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(debtCapacityQueueUrl)
                .messageBody(payload)
                .build();

        return Mono.fromFuture(() -> sqsAsyncClient.sendMessage(request)).then();
    }

}