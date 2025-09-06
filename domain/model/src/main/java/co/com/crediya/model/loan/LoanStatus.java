package co.com.crediya.model.loan;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanStatus {

    private UUID id;
    private String name;
    private String description;
}
