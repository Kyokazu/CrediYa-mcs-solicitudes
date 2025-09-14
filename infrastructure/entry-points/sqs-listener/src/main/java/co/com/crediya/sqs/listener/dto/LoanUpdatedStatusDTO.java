package co.com.crediya.sqs.listener.dto;


import java.util.UUID;

public record LoanUpdatedStatusDTO(
        UUID id,
        String status
) {
}
