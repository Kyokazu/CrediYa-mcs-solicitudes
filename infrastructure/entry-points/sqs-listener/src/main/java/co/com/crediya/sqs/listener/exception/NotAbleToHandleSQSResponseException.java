package co.com.crediya.sqs.listener.exception;

public class NotAbleToHandleSQSResponseException extends RuntimeException {
    public NotAbleToHandleSQSResponseException(String message) {
        super(message);
    }
}
