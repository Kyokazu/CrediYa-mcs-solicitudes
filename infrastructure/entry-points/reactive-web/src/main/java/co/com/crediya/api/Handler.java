package co.com.crediya.api;

import co.com.crediya.api.dto.LoanDTO;
import co.com.crediya.api.dto.LoanDetailsDTO;
import co.com.crediya.api.dto.UpdateLoanDTO;
import co.com.crediya.api.exception.MissingInvalidAuthHeaderException;
import co.com.crediya.api.exception.ValidationException;
import co.com.crediya.api.util.ValidatorUtil;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.usecase.manualloanreview.ManualLoanReviewUseCase;
import co.com.crediya.usecase.requestloan.RequestLoanUseCase;
import co.com.crediya.usecase.updateloanstatus.UpdateLoanStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final ValidatorUtil validatorUtil;
    private final TransactionalOperator txOperator;
    private final RequestLoanUseCase requestLoanUseCase;
    private final ManualLoanReviewUseCase manualLoanReviewUseCase;
    private final UpdateLoanStatusUseCase updateLoanStatusUseCase;

    public Mono<ServerResponse> saveLoan(ServerRequest request) {
        log.info("Received request to save loan");

        return extractToken(request)
                .flatMap(token -> processLoanRequest(request, token))
                .as(txOperator::transactional)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(ValidationException.class, this::handleValidationException)
                .doOnError(err -> log.error("Unexpected error while saving loan: {}", err.getMessage(), err));
    }

    public Mono<ServerResponse> getLoan(ServerRequest request) {
        log.info("Received request to get loan");
        return extractToken(request)
                .flatMap(token -> {
                    PageRequest pageRequest = extractPageRequest(request);
                    String filter = extractFilter(request);

                    log.info("📌 Fetching loans | page: {}, size: {}, filter: {} | token starts with: {}",
                            pageRequest.page(), pageRequest.size(), filter, token.substring(0, Math.min(10, token.length())));

                    return manualLoanReviewUseCase.getLoan(
                                    pageRequest.page(),
                                    pageRequest.size(),
                                    filter,
                                    token
                            )
                            .doOnNext(userLoanInfo -> log.debug("➡️ UserLoanInfo fetched: {}", userLoanInfo))
                            .map(this::mapToLoanDetailsDTO)
                            .doOnNext(dto -> log.debug("✅ Mapped to LoanDetailsDTO: {}", dto))
                            .collectList()
                            .flatMap(this::buildLoanListResponse);
                })
                .onErrorResume(ValidationException.class, this::handleValidationException)
                .doOnError(err -> log.error("❌ Unexpected error while fetching loans: {}", err.getMessage(), err));
    }

    public Mono<ServerResponse> updateLoan(ServerRequest request) {
        log.info("Received request to update loan");
        return extractToken(request)
                .flatMap(token -> updateLoanStatus(request, token))
                .as(txOperator::transactional)
                .flatMap(this::buildSuccessResponse)
                .onErrorResume(ValidationException.class, this::handleValidationException)
                .doOnError(err -> log.error("Unexpected error while updating loan: {}", err.getMessage(), err));
    }


    private LoanDetailsDTO mapToLoanDetailsDTO(UserLoanInfo userLoanInfo) {
        LoanDetailsDTO dto = new LoanDetailsDTO();
        dto.setUserId(userLoanInfo.getUserId());
        dto.setLoanId(userLoanInfo.getLoanId());
        dto.setAmount(userLoanInfo.getAmount());
        dto.setDuration(userLoanInfo.getDuration());
        dto.setEmail(userLoanInfo.getEmail());
        dto.setName(userLoanInfo.getName());
        dto.setLoanType(userLoanInfo.getLoanType());
        dto.setInterestRate(userLoanInfo.getInterestRate());
        dto.setLoanStatus(userLoanInfo.getLoanStatus());
        dto.setIncome(userLoanInfo.getIncome());
        dto.setMonthlyLoanPayment(userLoanInfo.getMonthlyLoanPayment());

        log.debug("🔄 Mapping UserLoanInfo -> LoanDetailsDTO | email={}, loanType={}, status={}",
                dto.getEmail(), dto.getLoanType(), dto.getLoanStatus());

        return dto;
    }

    private Mono<ServerResponse> buildLoanListResponse(List<LoanDetailsDTO> loans) {
        log.info("📊 Returning {} loans in response", loans.size());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loans);
    }

    private Mono<String> extractToken(ServerRequest request) {
        return Mono.justOrEmpty(request.headers().firstHeader(HttpHeaders.AUTHORIZATION))
                .doOnNext(header -> log.debug("🔐 Authorization header found: {}", header))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .map(authHeader -> {
                    String token = authHeader.substring(7);
                    log.info("🔑 Extracted JWT token (first 10 chars): {}", token.substring(0, Math.min(10, token.length())));
                    return token;
                })
                .switchIfEmpty(Mono.error(new MissingInvalidAuthHeaderException("Missing or invalid Authorization header")));
    }

    private Mono<Loan> processLoanRequest(ServerRequest request, String token) {
        return request.bodyToMono(LoanDTO.class)
                .doOnNext(dto -> log.info("📥 LoanDTO received: {}", dto))
                .flatMap(validatorUtil::validate)
                .doOnSuccess(dto -> log.info("✅ Validation successful for DTO with identification {}", dto.getIdentification()))
                .flatMap(dto -> {
                    Loan loan = mapToLoan(dto);
                    log.debug("🔄 Mapped LoanDTO to Loan: {}", loan);
                    return requestLoanUseCase.saveLoan(loan, dto.getLoanType(), token);
                });
    }

    private Mono<Loan> updateLoanStatus(ServerRequest request, String token) {
        return request.bodyToMono(UpdateLoanDTO.class)
                .doOnNext(dto -> log.info("📥 LoanDTO received: {}", dto))
                .flatMap(validatorUtil::validate)
                .doOnSuccess(dto -> log.info("✅ Validation successful for DTO with identification {}", dto.getId()))
                .flatMap(dto -> {
                    Loan loan = Loan.builder()
                            .id(dto.getId())
                            .email(dto.getStatus()).build();
                    log.debug("🔄 Mapped LoanDTO to Loan: {}", loan);
                    return updateLoanStatusUseCase.updateLoanStatus(loan, token);
                });


    }

    private Loan mapToLoan(LoanDTO dto) {
        Loan loan = Loan.builder()
                .email(dto.getIdentification())
                .amount(dto.getAmount())
                .duration(dto.getDuration())
                .build();
        log.debug("📌 Building Loan entity from DTO: {}", loan);
        return loan;
    }

    private Mono<ServerResponse> buildSuccessResponse(Loan savedLoan) {
        log.info("💾 Loan saved successfully with ID: {}", savedLoan.getId());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(savedLoan);
    }


    private Mono<ServerResponse> handleValidationException(ValidationException e) {
        log.warn("⚠️ Validation error: {}", e.getErrors());
        return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "status", e.getStatus(),
                        "error", "Validation errors",
                        "errors", e.getErrors(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    private record PageRequest(int page, int size) {
    }

    private PageRequest extractPageRequest(ServerRequest request) {
        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .orElse(0);
        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .orElse(10);

        log.info("📥 Extracted pagination params | page={}, size={}", page, size);
        return new PageRequest(page, size);
    }

    private String extractFilter(ServerRequest request) {
        String filter = request.queryParam("filter").orElse("ALL");
        log.info("📥 Extracted filter param: {}", filter);
        return filter;
    }


}
