package co.com.crediya.api.util;


import co.com.crediya.api.exception.MissingInvalidAuthHeaderException;
import co.com.crediya.api.exception.ValidationException;
import co.com.crediya.usecase.manualloanreview.exception.NotConsultantRoleException;
import co.com.crediya.usecase.requestloan.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationException(ValidationException ex) {
        return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "status", ex.getStatus(),
                "error", "Los datos de entrada están incompletos o inválidos.",
                "errors", ex.getErrors(),
                "timestamp", LocalDateTime.now().toString()
        )));
    }

    @ExceptionHandler({EmailNotFoundException.class, LoanRequesterException.class,
            LoanStatusNotFoundException.class, LoanTypeNotFoundException.class, NotConsultantRoleException.class,
            MissingInvalidAuthHeaderException.class, InvalidTokenException.class})
    public Mono<ResponseEntity<Map<String, Object>>> handleBusinessValidationExceptions(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        )));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Bad Request",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", ex.getMessage()
                ));
    }
}