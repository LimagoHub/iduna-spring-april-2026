package de.limago.onion.application.commandside.person.port.inport.person;

import de.limago.onion.application.queryside.query.person.FindPersonCompleteByIdQuery;
import de.limago.onion.application.queryside.query.person.PersonCompleteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPersonCompleteUseCase {
    Mono<PersonCompleteResult> findCompleteById(FindPersonCompleteByIdQuery query);
    Flux<PersonCompleteResult> findAllComplete();
}
