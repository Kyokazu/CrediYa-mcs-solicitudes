package co.com.crediya.usecase.requestloan;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.UserTokenInfo;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.requestloan.exception.EmailNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanRequesterException;
import co.com.crediya.usecase.requestloan.exception.LoanStatusNotFoundException;
import co.com.crediya.usecase.requestloan.exception.LoanTypeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoanUseCase Tests")
class RequestLoanUseCaseTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private LoanStatusRepository loanStatusRepository;

    @Mock
    private UserGateway userGateway;

    @Mock
    private JwtGateway jwtGateway;

    @InjectMocks
    private RequestLoanUseCase requestLoanUseCase;

    private static final String VALID_IDENTIFICATION = "12345678";
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_TOKEN = "valid-jwt-token";
    private static final String LOAN_TYPE_NAME = "PERSONAL";
    private static final String ROLE_NAME = "CLIENT";
    private static final UUID LOAN_TYPE_ID = UUID.randomUUID();
    private static final UUID LOAN_STATUS_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Reset all mocks before each test
        reset(loanRepository, loanTypeRepository, loanStatusRepository, userGateway, jwtGateway);
    }

    @Test
    @DisplayName("Should save loan successfully when all validations pass")
    void shouldSaveLoanSuccessfully() {
        // Given
        Loan inputLoan = createLoan();
        Loan savedLoan = createCompleteLoan();
        UserTokenInfo tokenInfo = createUserTokenInfo(VALID_EMAIL);

        // Setup all mocks
        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName(LOAN_TYPE_NAME))
                .thenReturn(Mono.just(LOAN_TYPE_ID));
        when(loanStatusRepository.getIdByName("PENDING"))
                .thenReturn(Mono.just(LOAN_STATUS_ID));
        when(loanRepository.saveLoan(any(Loan.class)))
                .thenReturn(Mono.just(savedLoan));

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .assertNext(result -> {
                    assertEquals(savedLoan.getEmail(), result.getEmail());
                    assertEquals(savedLoan.getLoanTypeId(), result.getLoanTypeId());
                    assertEquals(savedLoan.getLoanStatusId(), result.getLoanStatusId());
                })
                .verifyComplete();

        // Verify interactions
        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verify(jwtGateway).validateToken(VALID_TOKEN);
        verify(loanTypeRepository).getIdByName(LOAN_TYPE_NAME);
        verify(loanStatusRepository).getIdByName("PENDING");
        verify(loanRepository).saveLoan(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw EmailNotFoundException when email is not found")
    void shouldThrowEmailNotFoundExceptionWhenEmailNotFound() {
        // Given
        Loan inputLoan = createLoan();

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectError(EmailNotFoundException.class)
                .verify();

        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verifyNoInteractions(jwtGateway, loanRepository, loanTypeRepository, loanStatusRepository);
    }

    @Test
    @DisplayName("Should throw RuntimeException when token is invalid")
    void shouldThrowRuntimeExceptionWhenTokenIsInvalid() {
        // Given
        Loan inputLoan = createLoan();

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.empty());

        // Mocks adicionales para evitar NPE
        when(loanTypeRepository.getIdByName(anyString()))
                .thenReturn(Mono.just(UUID.randomUUID()));
        when(loanStatusRepository.getIdByName(anyString()))
                .thenReturn(Mono.just(UUID.randomUUID()));

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Invalid token"))
                .verify();

        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verify(jwtGateway).validateToken(VALID_TOKEN);
        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should throw LoanRequesterException when token email differs from identification email")
    void shouldThrowLoanRequesterExceptionWhenEmailsDiffer() {
        // Given
        Loan inputLoan = createLoan();
        String differentEmail = "different@example.com";
        UserTokenInfo tokenInfo = createUserTokenInfo(differentEmail);

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));

        // Mocks adicionales para evitar NPE
        when(loanTypeRepository.getIdByName(anyString()))
                .thenReturn(Mono.just(UUID.randomUUID()));
        when(loanStatusRepository.getIdByName(anyString()))
                .thenReturn(Mono.just(UUID.randomUUID()));

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanRequesterException &&
                                throwable.getMessage().equals("The loan requester identity is different from the logger user"))
                .verify();

        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verify(jwtGateway).validateToken(VALID_TOKEN);
        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should throw LoanTypeNotFoundException when loan type is not found")
    void shouldThrowLoanTypeNotFoundExceptionWhenLoanTypeNotFound() {
        // Given
        Loan inputLoan = createLoan();
        UserTokenInfo tokenInfo = createUserTokenInfo(VALID_EMAIL);

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName(LOAN_TYPE_NAME))
                .thenReturn(Mono.empty());
        when(loanStatusRepository.getIdByName("PENDING"))
                .thenReturn(Mono.just(LOAN_STATUS_ID));

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanTypeNotFoundException &&
                                throwable.getMessage().contains("LoanType not found"))
                .verify();

        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verify(jwtGateway).validateToken(VALID_TOKEN);
        verify(loanTypeRepository).getIdByName(LOAN_TYPE_NAME);
        verify(loanStatusRepository).getIdByName("PENDING");
        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should throw LoanStatusNotFoundException when pending status is not found")
    void shouldThrowLoanStatusNotFoundExceptionWhenPendingStatusNotFound() {
        // Given
        Loan inputLoan = createLoan();
        UserTokenInfo tokenInfo = createUserTokenInfo(VALID_EMAIL);

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName(LOAN_TYPE_NAME))
                .thenReturn(Mono.just(LOAN_TYPE_ID));
        when(loanStatusRepository.getIdByName("PENDING"))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanStatusNotFoundException &&
                                throwable.getMessage().contains("LoanStatus not found"))
                .verify();

        verify(userGateway).getEmailByIdentification(VALID_IDENTIFICATION);
        verify(jwtGateway).validateToken(VALID_TOKEN);
        verify(loanTypeRepository).getIdByName(LOAN_TYPE_NAME);
        verify(loanStatusRepository).getIdByName("PENDING");
        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should handle repository save error")
    void shouldHandleRepositorySaveError() {
        // Given
        Loan inputLoan = createLoan();
        UserTokenInfo tokenInfo = createUserTokenInfo(VALID_EMAIL);
        RuntimeException saveError = new RuntimeException("Database error");

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName(LOAN_TYPE_NAME))
                .thenReturn(Mono.just(LOAN_TYPE_ID));
        when(loanStatusRepository.getIdByName("PENDING"))
                .thenReturn(Mono.just(LOAN_STATUS_ID));
        when(loanRepository.saveLoan(any(Loan.class)))
                .thenReturn(Mono.error(saveError));

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(loanRepository).saveLoan(any(Loan.class));
    }

    @Test
    @DisplayName("Should build loan correctly with proper values")
    void shouldBuildLoanCorrectlyWithProperValues() {
        // Given
        Loan inputLoan = createLoan();
        UserTokenInfo tokenInfo = createUserTokenInfo(VALID_EMAIL);

        when(userGateway.getEmailByIdentification(VALID_IDENTIFICATION))
                .thenReturn(Mono.just(VALID_EMAIL));
        when(jwtGateway.validateToken(VALID_TOKEN))
                .thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName(LOAN_TYPE_NAME))
                .thenReturn(Mono.just(LOAN_TYPE_ID));
        when(loanStatusRepository.getIdByName("PENDING"))
                .thenReturn(Mono.just(LOAN_STATUS_ID));
        when(loanRepository.saveLoan(any(Loan.class)))
                .thenAnswer(invocation -> {
                    Loan loanToSave = invocation.getArgument(0);
                    return Mono.just(loanToSave);
                });

        // When & Then
        StepVerifier.create(requestLoanUseCase.saveLoan(inputLoan, LOAN_TYPE_NAME, VALID_TOKEN))
                .assertNext(savedLoan -> {
                    assertEquals(VALID_EMAIL, savedLoan.getEmail());
                    assertEquals(LOAN_TYPE_ID, savedLoan.getLoanTypeId());
                    assertEquals(LOAN_STATUS_ID, savedLoan.getLoanStatusId());
                    assertEquals(BigDecimal.valueOf(50000), savedLoan.getAmount());
                })
                .verifyComplete();
    }

    // Helper methods
    private Loan createLoan() {
        return Loan.builder()
                .id(UUID.randomUUID())
                .email(VALID_IDENTIFICATION)
                .amount(BigDecimal.valueOf(50000))
                .build();
    }

    private Loan createCompleteLoan() {
        return Loan.builder()
                .id(UUID.randomUUID())
                .email(VALID_EMAIL)
                .amount(BigDecimal.valueOf(50000))
                .loanTypeId(LOAN_TYPE_ID)
                .loanStatusId(LOAN_STATUS_ID)
                .build();
    }

    private UserTokenInfo createUserTokenInfo(String email) {
        return UserTokenInfo.builder()
                .email(email)
                .token(VALID_TOKEN)
                .role(ROLE_NAME)
                .build();
    }
}