package co.com.crediya.api.util;


import co.com.crediya.api.dto.ApiErrorDTO;
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

    @ExceptionHandler({EmailNotFoundException.class, LoanStatusNotFoundException.class,
            LoanTypeNotFoundException.class})
    public Mono<ResponseEntity<ApiErrorDTO>> handleBusinessNotFoundExceptions(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Not found",
                ex.getMessage(),
                LocalDateTime.now().toString()
        )));
    }

    @ExceptionHandler({NotEnoughDebtCapacityException.class})
    public Mono<ResponseEntity<ApiErrorDTO>> handleBusinessDebtExceptions(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Not enough debt capacity",
                ex.getMessage(),
                LocalDateTime.now().toString()
        )));
    }

    @ExceptionHandler({LoanRequesterException.class, NotConsultantRoleException.class})
    public Mono<ResponseEntity<ApiErrorDTO>> handleBusinessValidationExceptions(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiErrorDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                LocalDateTime.now().toString()
        )));
    }

    @ExceptionHandler({MissingInvalidAuthHeaderException.class, InvalidTokenException.class})
    public Mono<ResponseEntity<ApiErrorDTO>> handleAuthorizationValidationExceptions(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(new ApiErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Missing Authorization",
                ex.getMessage(),
                LocalDateTime.now().toString()
        )));
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorDTO> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        "Not Found",
                        ex.getMessage(),
                        LocalDateTime.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        "Not Found",
                        ex.getMessage(),
                        LocalDateTime.now().toString()));
    }
}