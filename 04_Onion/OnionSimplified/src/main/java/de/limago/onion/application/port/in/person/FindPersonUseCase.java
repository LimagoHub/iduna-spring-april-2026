package de.limago.onion.application.port.in.person;

import de.limago.onion.application.query.person.FindPersonByIdQuery;
import de.limago.onion.application.query.person.PersonResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPersonUseCase {
    Mono<PersonResult> findById(FindPersonByIdQuery query);
    Flux<PersonResult> findAll();
}
