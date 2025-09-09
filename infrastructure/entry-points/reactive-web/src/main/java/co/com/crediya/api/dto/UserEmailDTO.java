package co.com.crediya.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserEmailDTO {
    private String email;
}