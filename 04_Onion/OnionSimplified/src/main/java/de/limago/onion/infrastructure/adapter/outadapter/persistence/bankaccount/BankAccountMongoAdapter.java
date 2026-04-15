package de.limago.onion.infrastructure.adapter.outadapter.persistence.bankaccount;

import de.limago.onion.application.commandside.person.port.outport.BankAccountRepository;
import de.limago.onion.domain.person.aggregate.BankAccountAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BankAccountMongoAdapter implements BankAccountRepository {

    private final SpringBankAccountRepository springRepo;
    private final BankAccountDocumentMapper mapper;

    @Override
    public Mono<Void> save(BankAccountAggregate bankAccount) {
        return springRepo.save(mapper.toDocument(bankAccount)).then();
    }

    @Override
    public Mono<BankAccountAggregate> findByPersonId(UUID personId) {
        return springRepo.findById(personId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteByPersonId(UUID personId) {
        return springRepo.deleteById(personId.toString());
    }
}
