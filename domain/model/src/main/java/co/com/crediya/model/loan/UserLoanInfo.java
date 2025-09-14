package co.com.crediya.model.loan;


import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserLoanInfo {

    private UUID userId;
    private UUID loanId;
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
