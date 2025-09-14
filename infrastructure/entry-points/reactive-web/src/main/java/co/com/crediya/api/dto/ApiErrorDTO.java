package co.com.crediya.api.dto;

public record ApiErrorDTO(
        int status,
        String error,
        String message,
        String timestamp
) {
}
