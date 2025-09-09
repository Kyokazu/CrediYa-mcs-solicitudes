package co.com.crediya.api.exception;

public class MissingInvalidAuthHeaderException extends RuntimeException {
    public MissingInvalidAuthHeaderException(String message) {
        super(message);
    }
}
