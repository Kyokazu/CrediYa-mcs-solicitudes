package co.com.crediya.usecase.requestloan.exception;

public class LoanStatusNotFoundException extends RuntimeException {
    public LoanStatusNotFoundException(String message) {
        super(message);
    }
}
