package co.com.crediya.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantGetLoanDTO {

    private int page;
    private int size;
    private String filter;

}
