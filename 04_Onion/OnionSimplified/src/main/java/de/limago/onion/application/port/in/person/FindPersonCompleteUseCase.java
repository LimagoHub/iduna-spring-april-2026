package de.limago.onion.application.port.in.person;

import de.limago.onion.application.query.person.FindPersonCompleteByIdQuery;
import de.limago.onion.application.query.person.PersonCompleteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPersonCompleteUseCase {
    Mono<PersonCompleteResult> findCompleteById(FindPersonCompleteByIdQuery query);
    Flux<PersonCompleteResult> findAllComplete();
}
