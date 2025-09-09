package co.com.crediya.model.loan;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Loan {

    private UUID id;
    private BigDecimal amount;
    private Long duration;
    private String email;
    private UUID loanStatusId;
    private UUID loanTypeId;

}
