package de.limago.onion.application.commandside.person.port.inport.person;

import de.limago.onion.application.queryside.query.person.FindPersonByIdQuery;
import de.limago.onion.application.queryside.query.person.PersonResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPersonUseCase {
    Mono<PersonResult> findById(FindPersonByIdQuery query);
    Flux<PersonResult> findAll();
}
