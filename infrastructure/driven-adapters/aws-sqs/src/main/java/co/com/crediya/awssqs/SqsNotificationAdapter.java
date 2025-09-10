package co.com.crediya.awssqs;

import co.com.crediya.model.loan.gateways.NotificationQueueGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;


@Service
@RequiredArgsConstructor
public class SqsNotificationAdapter implements NotificationQueueGateway {

    private final SqsAsyncClient sqsAsyncClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Override
    public Mono<Void> sendMessage(String email, String message) {
        String payload = String.format("{\"email\":\"%s\", \"body\":\"%s\"}", email, message);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payload)
                .build();

        return Mono.fromFuture(() -> sqsAsyncClient.sendMessage(request)).then();
    }

}