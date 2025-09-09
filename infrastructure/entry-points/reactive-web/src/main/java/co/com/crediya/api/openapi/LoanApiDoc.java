package co.com.crediya.api.openapi;

import co.com.crediya.api.dto.LoanDTO;
import co.com.crediya.api.dto.LoanDetailsDTO;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import org.springdoc.core.fn.builders.operation.Builder;

@UtilityClass
public class LoanApiDoc {

    public Builder saveLoanDoc(Builder builder) {
        return builder
                .operationId("saveLoan")
                .description("Create a new loan")
                .tag("Loan")
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(LoanDTO.class))))
                .response(responseBuilder().responseCode("201").description("Loan created successfully")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().type("string"))))
                .response(responseBuilder().responseCode("400").description("Invalid input data")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().type("string"))));
    }

    public Builder getLoanDoc(Builder builder) {
        return builder
                .operationId("getLoan")
                .description("Retrieve loans with pagination and optional filter")
                .tag("Loan")
                .response(responseBuilder().responseCode("200").description("Loans retrieved successfully")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(LoanDetailsDTO.class).type("array"))))
                .response(responseBuilder().responseCode("404").description("No loans found")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().type("string"))));
    }
}