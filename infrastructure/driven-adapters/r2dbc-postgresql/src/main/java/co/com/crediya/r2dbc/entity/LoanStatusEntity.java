package co.com.crediya.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("loan_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class LoanStatusEntity {

    @Id
    @Column("id")
    private UUID id;
    private String name;
    private String description;
}
