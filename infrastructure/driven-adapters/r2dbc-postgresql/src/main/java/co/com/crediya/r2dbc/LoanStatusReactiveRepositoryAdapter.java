package co.com.crediya.r2dbc;

import co.com.crediya.model.loan.LoanStatus;
import co.com.crediya.model.loan.gateways.LoanStatusRepository;

import co.com.crediya.r2dbc.entity.LoanStatusEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class LoanStatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<LoanStatus, LoanStatusEntity, UUID, LoanStatusReactiveRepository> implements LoanStatusRepository {


    private static final Logger log = LoggerFactory.getLogger(LoanStatusReactiveRepositoryAdapter.class);

    public LoanStatusReactiveRepositoryAdapter(LoanStatusReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, entity -> mapper.map(entity, LoanStatus.class));
    }


    @Override
    public Mono<UUID> getIdByName(String name) {
        LoanStatus loan = LoanStatus.builder()
                .name(name).build();
        return super.findByExample(loan)
                .next()
                .map(LoanStatus::getId)
                .switchIfEmpty(Mono.empty())
                .doOnSuccess(id -> log.info("📌 Loan Status name= {} -> Loan Status id= {}", name, id));
    }

    @Override
    public Mono<String> getNameById(UUID id) {
        LoanStatus loan = LoanStatus.builder()
                .id(id).build();
        return super.findByExample(loan)
                .next()
                .map(LoanStatus::getName)
                .switchIfEmpty(Mono.empty())
                .doOnSuccess(name -> log.info("📌 Loan Status name= {} -> Loan Status id= {}", name, id));

    }
}

