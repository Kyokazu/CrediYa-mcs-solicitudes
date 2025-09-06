package co.com.crediya.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("loan_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanTypeEntity {

    @Id
    @Column("id")
    private UUID id;
    private String name;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Double interestRate;
    private Boolean automaticValidation;

}
