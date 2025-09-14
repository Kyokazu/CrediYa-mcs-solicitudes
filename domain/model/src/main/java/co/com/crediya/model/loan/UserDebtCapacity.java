package co.com.crediya.model.loan;

import java.math.BigDecimal;
import java.util.UUID;

public record UserDebtCapacity(
        UUID id,
        String email,
        Double interestRate,
        Long duration,
        BigDecimal income,
        BigDecimal amount,
        BigDecimal monthlyDebt
) {
}
