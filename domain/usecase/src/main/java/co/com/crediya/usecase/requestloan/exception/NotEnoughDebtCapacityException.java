package co.com.crediya.usecase.requestloan.exception;

public class NotEnoughDebtCapacityException extends RuntimeException {
    public NotEnoughDebtCapacityException(String message) {
        super(message);
    }
}
