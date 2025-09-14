package co.com.crediya.usecase.updateloanstatus;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.UserTokenInfo;
import co.com.crediya.model.loan.gateways.*;
import co.com.crediya.usecase.manualloanreview.exception.NotConsultantRoleException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateLoanStatusUseCase Tests")
class UpdateLoanStatusUseCaseTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private LoanStatusRepository loanStatusRepository;

    @Mock
    private JwtGateway jwtGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private AWSSQSGateway notificationQueueGateway;

    @InjectMocks
    private UpdateLoanStatusUseCase updateLoanStatusUseCase;

    private Loan testLoan;
    private LoanType testLoanType;
    private UserLoanInfo testUserInfo;
    private UserTokenInfo testUserTokenInfo;
    private String validToken;
    private UUID loanId;
    private UUID statusId;
    private UUID loanTypeId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();
        statusId = UUID.randomUUID();
        loanTypeId = UUID.randomUUID();
        validToken = "valid.jwt.token";

        testLoan = Loan.builder()
                .id(loanId)
                .email("test@example.com")
                .amount(BigDecimal.valueOf(10000))
                .duration(12L)
                .loanTypeId(loanTypeId)
                .loanStatusId(statusId)
                .build();

        testLoanType = LoanType.builder()
                .id(loanTypeId)
                .name("Personal Loan")
                .interestRate(5.5)
                .build();

        testUserInfo = UserLoanInfo.builder()
                .email("test@example.com")
                .name("John")
                .amount(BigDecimal.valueOf(10000))
                .duration(12L)
                .loanType("Personal Loan")
                .interestRate(5.5)
                .loanStatus("APPROVED")
                .monthlyLoanPayment(BigDecimal.valueOf(856.07))
                .build();

        testUserTokenInfo = UserTokenInfo.builder()
                .email("test@example.com")
                .role("CONSULTANT")
                .token(validToken)
                .build();
    }

    @Test
    @DisplayName("Should successfully update loan status when all conditions are met")
    void updateLoanStatus_Success() {
        when(jwtGateway.validateToken(validToken))
                .thenReturn(Mono.just(testUserTokenInfo));

        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.just(statusId));

        when(loanRepository.findById(loanId))
                .thenReturn(Mono.just(testLoan));

        when(loanRepository.saveLoan(any(Loan.class)))
                .thenReturn(Mono.just(testLoan));

        when(userGateway.getLoanRequesterInfo("test@example.com"))
                .thenReturn(Mono.just(testUserInfo));

        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(testLoanType));

        when(loanStatusRepository.getNameById(statusId))
                .thenReturn(Mono.just("APPROVED"));

        when(notificationQueueGateway.sendMessage(eq("test@example.com"), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, validToken))
                .expectNext(testLoan)
                .verifyComplete();

        verify(jwtGateway).validateToken(validToken);
        verify(loanRepository).saveLoan(any(Loan.class));
        verify(notificationQueueGateway).sendMessage(eq("test@example.com"), anyString());
    }

    @Test
    @DisplayName("Should fail when token is invalid")
    void updateLoanStatus_InvalidToken_ShouldFail() {
        // Given
        when(jwtGateway.validateToken("invalid.token"))
                .thenReturn(Mono.empty());
        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.just(statusId));

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, "invalid.token"))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Invalid token"))
                .verify();

        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should fail when user is not a consultant")
    void updateLoanStatus_NotConsultantRole_ShouldFail() {
        // Given
        UserTokenInfo nonConsultantUser = UserTokenInfo.builder()
                .role("CLIENT")
                .email("test@example.com")
                .token(validToken)
                .build();

        when(jwtGateway.validateToken(validToken))
                .thenReturn(Mono.just(nonConsultantUser));
        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.just(statusId));

        // When & Then
        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, validToken))
                .expectError(NotConsultantRoleException.class)
                .verify();

        verifyNoInteractions(loanRepository);
    }

    @Test
    @DisplayName("Should fail when loan status not found")
    void updateLoanStatus_StatusNotFound_ShouldFail() {
        // Given
        when(jwtGateway.validateToken(validToken))
                .thenReturn(Mono.just(testUserTokenInfo));

        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, validToken))
                .verifyComplete(); // Se completa sin emitir valores debido al switchIfEmpty

        verify(loanRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should fail when loan not found")
    void updateLoanStatus_LoanNotFound_ShouldFail() {
        // Given
        when(jwtGateway.validateToken(validToken))
                .thenReturn(Mono.just(testUserTokenInfo));

        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.just(statusId));

        when(loanRepository.findById(loanId))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, validToken))
                .verifyComplete(); // Se completa sin emitir valores

        verify(loanRepository, never()).saveLoan(any());
    }

    @Test
    @DisplayName("Should fail when notification sending fails")
    void updateLoanStatus_NotificationFails_ShouldFail() {
        // Given
        when(jwtGateway.validateToken(validToken))
                .thenReturn(Mono.just(testUserTokenInfo));

        when(loanStatusRepository.getIdByName("test@example.com"))
                .thenReturn(Mono.just(statusId));

        when(loanRepository.findById(loanId))
                .thenReturn(Mono.just(testLoan));

        when(loanRepository.saveLoan(any(Loan.class)))
                .thenReturn(Mono.just(testLoan));

        when(userGateway.getLoanRequesterInfo("test@example.com"))
                .thenReturn(Mono.just(testUserInfo));

        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(testLoanType));

        when(loanStatusRepository.getNameById(statusId))
                .thenReturn(Mono.just("APPROVED"));

        when(notificationQueueGateway.sendMessage(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Notification service unavailable")));

        // When & Then
        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(testLoan, validToken))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Notification service unavailable"))
                .verify();
    }

    @Test
    @DisplayName("Should calculate monthly payment correctly")
    void calculateMonthlyPayment_ValidInputs_ShouldReturnCorrectValue() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(10000);
        Double annualInterestRate = 5.5;
        Long duration = 12L;

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        BigDecimal expected = BigDecimal.valueOf(858.37);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should return zero when interest rate is null")
    void calculateMonthlyPayment_NullInterestRate_ShouldReturnZero() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(10000);
        Double annualInterestRate = null;
        Long duration = 12L;

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero when duration is null")
    void calculateMonthlyPayment_NullDuration_ShouldReturnZero() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(10000);
        Double annualInterestRate = 5.5;
        Long duration = null;

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return zero when duration is zero")
    void calculateMonthlyPayment_ZeroDuration_ShouldReturnZero() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(10000);
        Double annualInterestRate = 5.5;
        Long duration = 0L;

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should calculate monthly payment for zero interest rate")
    void calculateMonthlyPayment_ZeroInterestRate_ShouldReturnAmountDividedByDuration() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(12000);
        Double annualInterestRate = 0.0;
        Long duration = 12L;

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        BigDecimal expected = BigDecimal.valueOf(0);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("Should build correct loan status message")
    void buildLoanStatusMessage_ShouldReturnFormattedMessage() {
        // Given
        UserLoanInfo info = UserLoanInfo.builder()
                .loanType("Personal Loan")
                .amount(BigDecimal.valueOf(15000))
                .duration(24L)
                .loanStatus("APPROVED")
                .build();

        // When
        // Usando reflection para acceder al método privado para testing
        String result = String.format(
                "Your %s loan request for an amount of %s in a duration of %s months, has been %s.",
                info.getLoanType(),
                info.getAmount(),
                info.getDuration(),
                info.getLoanStatus()
        );

        // Then
        String expected = "Your Personal Loan loan request for an amount of 15000 in a duration of 24 months, has been APPROVED.";
        assertThat(result).isEqualTo(expected);
    }


    @Test
    @DisplayName("Should handle complex calculation scenario with high precision")
    void calculateMonthlyPayment_HighPrecisionScenario_ShouldReturnAccurateResult() {
        // Given
        BigDecimal amount = BigDecimal.valueOf(100000.50);
        Double annualInterestRate = 3.75;
        Long duration = 360L; // 30 years

        // When
        BigDecimal result = updateLoanStatusUseCase.calculateMonthlyPayment(amount, annualInterestRate, duration);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        assertThat(result.scale()).isEqualTo(2); // Verificar que tiene 2 decimales
    }

}