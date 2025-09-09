package co.com.crediya.consumer.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    private String name;
    private String email;
    private BigDecimal income;
}
