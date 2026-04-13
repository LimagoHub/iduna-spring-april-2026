package de.limago.onion.application.port.outport;

import de.limago.onion.domain.model.aggregate.PersonAggregate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PersonRepository {
    Mono<Void> save(PersonAggregate person);
    Mono<PersonAggregate> findById(UUID id);
    Flux<PersonAggregate> findAll();
}
