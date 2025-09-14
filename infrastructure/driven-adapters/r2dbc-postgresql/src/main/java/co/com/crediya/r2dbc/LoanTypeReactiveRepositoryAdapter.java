package co.com.crediya.r2dbc;

import co.com.crediya.model.loan.LoanStatus;
import co.com.crediya.model.loan.LoanType;
import co.com.crediya.model.loan.gateways.LoanTypeRepository;
import co.com.crediya.r2dbc.entity.LoanTypeEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LoanTypeReactiveRepositoryAdapter extends ReactiveAdapterOperations<LoanType, LoanTypeEntity, UUID, LoanTypeReactiveRepository> implements LoanTypeRepository {

    private static final Logger log = LoggerFactory.getLogger(LoanTypeReactiveRepositoryAdapter.class);

    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, entity -> mapper.map(entity, LoanType.class));

    }


    public Mono<LoanType> getLoanTypeByIdOrName(UUID id, String name) {
        LoanType loan = LoanType.builder()
                .id(id)
                .name(name)
                .build();

        return super.findByExample(loan)
                .next()
                .map(lt -> LoanType.builder()
                        .id(lt.getId())
                        .name(lt.getName())
                        .interestRate(lt.getInterestRate())
                        .automaticValidation(lt.getAutomaticValidation())
                        .maximumAmount(lt.getMaximumAmount())
                        .minimumAmount(lt.getMinimumAmount())
                        .build());
    }

    public Mono<LoanType> getLoanTypeById(UUID id) {
        return getLoanTypeByIdOrName(id, null);
    }

    public Mono<LoanType> getLoanTypeByName(String name) {
        return getLoanTypeByIdOrName(null, name);
    }
}
