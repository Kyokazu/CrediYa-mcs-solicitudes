package co.com.crediya.model.loan;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanType {

    private UUID id;
    private String name;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Double interestRate;
    private Boolean automaticValidation;


}
