package co.com.crediya.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetailsDTO {

    private BigDecimal amount;
    private Long duration;
    private String email;
    private String name;
    private String loanType;
    private Double interestRate;
    private String loanStatus;
    private BigDecimal income;
    private BigDecimal monthlyLoanPayment;
}
