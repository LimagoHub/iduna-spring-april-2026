package de.limago.onion.application.port.outport;

import de.limago.onion.domain.model.aggregate.BankAccountAggregate;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BankAccountRepository {
    Mono<Void> save(BankAccountAggregate bankAccount);
    Mono<BankAccountAggregate> findByPersonId(UUID personId);
    Mono<Void> deleteByPersonId(UUID personId);
}
