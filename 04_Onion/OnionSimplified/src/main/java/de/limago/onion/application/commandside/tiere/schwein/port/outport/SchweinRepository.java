package de.limago.onion.application.commandside.tiere.schwein.port.outport;

import de.limago.onion.domain.tiere.schwein.aggregate.Schwein;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SchweinRepository {
    Mono<Void> save(Schwein schwein);
    Mono<Schwein> findById(UUID id);
    Flux<Schwein> findAll();
}
