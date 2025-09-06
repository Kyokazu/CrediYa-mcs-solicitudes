package co.com.crediya.api.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {
    private final int status;
    private final List<String> errors;

    public ValidationException(List<String> errors, int status) {
        super("Errores de validación");
        this.errors = errors;
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }
}

