package co.com.crediya.usecase.manualloanreview;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.UserLoanInfo;
import co.com.crediya.model.loan.UserTokenInfo;
import co.com.crediya.model.loan.gateways.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

class ManualLoanReviewUseCaseTest {

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

    @InjectMocks
    private ManualLoanReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetLoan_validConsultant_allFilter() {
        // Datos de prueba
        UUID loanStatusId = UUID.randomUUID();
        UUID loanTypeId = UUID.randomUUID();

        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setAmount(BigDecimal.valueOf(1200));
        loan.setDuration(12L);
        loan.setEmail("user@example.com");
        loan.setLoanStatusId(loanStatusId);
        loan.setLoanTypeId(loanTypeId);

        LoanType loanType = new LoanType();
        loanType.setId(loanTypeId);
        loanType.setName("CONSULTANT");
        loanType.setInterestRate(12.0);

        UserLoanInfo userInfo = new UserLoanInfo();
        userInfo.setEmail("user@example.com");

        String loanStatusName = "PENDING";

        UserTokenInfo tokenInfo = new UserTokenInfo("validToken", "consultant@example.com", "CONSULTANT");

        // Mocks
        when(jwtGateway.validateToken("validToken")).thenReturn(Mono.just(tokenInfo));
        when(loanRepository.findAllPaged(0, 10)).thenReturn(Flux.just(loan));
        when(userGateway.getLoanRequesterInfo("user@example.com")).thenReturn(Mono.just(userInfo));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(loanStatusRepository.getNameById(loanStatusId)).thenReturn(Mono.just(loanStatusName));

        // Verificación
        StepVerifier.create(useCase.getLoan(0, 10, "ALL", "validToken"))
                .expectNextMatches(info ->
                        info.getEmail().equals("user@example.com") &&
                                info.getLoanType().equals("CONSULTANT") &&
                                info.getLoanStatus().equals("PENDING") &&
                                info.getMonthlyLoanPayment().compareTo(BigDecimal.ZERO) > 0
                )
                .verifyComplete();
    }

    @Test
    void testGetLoan_typeFilter() {
        UUID loanTypeId = UUID.randomUUID();
        UUID loanStatusId = UUID.randomUUID();

        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setAmount(BigDecimal.valueOf(1500));
        loan.setDuration(24L);
        loan.setEmail("user2@example.com");
        loan.setLoanStatusId(loanStatusId);
        loan.setLoanTypeId(loanTypeId);

        LoanType loanType = new LoanType();
        loanType.setId(loanTypeId);
        loanType.setName("MORTAGAGE");
        loanType.setInterestRate(10.0);

        UserLoanInfo userInfo = new UserLoanInfo();
        userInfo.setEmail("user2@example.com");

        String loanStatusName = "APPROVED";

        UserTokenInfo tokenInfo = new UserTokenInfo("validToken", "consultant@example.com", "CONSULTANT");

        // Mocks necesarios
        when(jwtGateway.validateToken("validToken")).thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName("MORTAGAGE")).thenReturn(Mono.just(loanTypeId));
        when(loanRepository.findAllPaged(0, 10)).thenReturn(Flux.just(loan));
        when(userGateway.getLoanRequesterInfo("user2@example.com")).thenReturn(Mono.just(userInfo));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(loanStatusRepository.getNameById(loanStatusId)).thenReturn(Mono.just(loanStatusName));

        StepVerifier.create(useCase.getLoan(0, 10, "MORTAGAGE", "validToken"))
                .expectNextMatches(info ->
                        info.getEmail().equals("user2@example.com") &&
                                info.getLoanType().equals("MORTAGAGE") &&
                                info.getLoanStatus().equals("APPROVED") &&
                                info.getMonthlyLoanPayment().compareTo(BigDecimal.ZERO) > 0
                )
                .verifyComplete();
    }

    @Test
    void testGetLoan_accessDenied() {
        UserTokenInfo tokenInfo = new UserTokenInfo("invalidToken", "user@example.com", "CLIENT");

        // Mocks
        when(jwtGateway.validateToken("invalidToken")).thenReturn(Mono.just(tokenInfo));

        StepVerifier.create(useCase.getLoan(0, 10, "ALL", "invalidToken"))
                .expectErrorMessage("Access denied: User does not have CONSULTANT role")
                .verify();
    }

    @Test
    void testGetLoan_filterNoResults() {
        UserTokenInfo tokenInfo = new UserTokenInfo("validToken", "consultant@example.com", "CONSULTANT");

        // Mocks
        when(jwtGateway.validateToken("validToken")).thenReturn(Mono.just(tokenInfo));
        when(loanTypeRepository.getIdByName("UnknownType")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getLoan(0, 10, "UnknownType", "validToken"))
                .verifyComplete(); // no loans encontrados
    }

    @Test
    void testCalculateMonthlyPayment_nonZero() {
        BigDecimal amount = BigDecimal.valueOf(1200);
        Double interestRate = 12.0;
        Long duration = 12L;

        BigDecimal payment = useCase.calculateMonthlyPayment(amount, interestRate, duration);

        assertTrue(payment.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateMonthlyPayment_zeroInterest() {
        BigDecimal payment = useCase.calculateMonthlyPayment(BigDecimal.valueOf(1000), 0.0, 12L);
        assertEquals(BigDecimal.ZERO, payment);
    }
}
