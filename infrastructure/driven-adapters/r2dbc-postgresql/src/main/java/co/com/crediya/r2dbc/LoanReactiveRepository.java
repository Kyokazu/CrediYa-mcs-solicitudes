package co.com.crediya.r2dbc;

import co.com.crediya.r2dbc.entity.LoanEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;


public interface LoanReactiveRepository extends ReactiveCrudRepository<LoanEntity, UUID>, ReactiveQueryByExampleExecutor<LoanEntity> {

}
