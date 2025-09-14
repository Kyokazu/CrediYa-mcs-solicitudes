package co.com.crediya.consumer.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    private UUID id;
    private String name;
    private String email;
    private BigDecimal income;
}
