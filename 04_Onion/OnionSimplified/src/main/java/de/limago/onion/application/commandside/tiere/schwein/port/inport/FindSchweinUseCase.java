package de.limago.onion.application.commandside.tiere.schwein.port.inport;

import de.limago.onion.application.queryside.query.schwein.FindSchweinByIdQuery;
import de.limago.onion.application.queryside.query.schwein.SchweinResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindSchweinUseCase {
    Mono<SchweinResult> findById(FindSchweinByIdQuery query);
    Flux<SchweinResult> findAll();
}
