package co.com.crediya.api.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanListDTO {

    @NotBlank(message = "Identification number is mandatory")
    private String identification;

    @NotNull(message = "Amount is mandatory")
    private BigDecimal amount;

    @NotNull(message = "Duration is mandatory")
    private Long duration;

    private String email;

    private String loanStatus;

    @NotBlank(message = "Loan Type is mandatory")
    @Pattern(regexp = "^(FREE|MORTGAGE|VEHICLE)$", message = "loan_type debe ser FREE, MORTGAGE o VEHICLE")
    private String loanType;
}
