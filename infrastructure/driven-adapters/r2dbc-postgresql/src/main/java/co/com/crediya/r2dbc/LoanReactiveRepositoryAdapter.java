package co.com.crediya.r2dbc;

import co.com.crediya.model.loan.Loan;
import co.com.crediya.model.loan.gateways.LoanRepository;
import co.com.crediya.r2dbc.entity.LoanEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<Loan, LoanEntity, UUID, LoanReactiveRepository> implements LoanRepository {

    private static final Logger log = LoggerFactory.getLogger(LoanReactiveRepositoryAdapter.class);

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, entity -> mapper.map(entity, Loan.class));
    }

    @Override
    public Mono<Loan> saveLoan(Loan loan) {
        return super.save(loan);
    }

    @Override
    public Flux<Loan> findAllPaged(int page, int size) {
        return super.findAllPaged(page, size);
    }

    @Override
    public Mono<Loan> findById(UUID id) {
        return super.findById(id);
    }

}
