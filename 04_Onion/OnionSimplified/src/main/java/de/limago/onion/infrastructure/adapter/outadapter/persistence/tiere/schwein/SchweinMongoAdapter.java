package de.limago.onion.infrastructure.adapter.outadapter.persistence.tiere.schwein;

import de.limago.onion.application.commandside.tiere.schwein.port.outport.SchweinRepository;
import de.limago.onion.domain.tiere.schwein.aggregate.Schwein;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SchweinMongoAdapter implements SchweinRepository {

    private final SpringSchweinRepository springRepo;
    private final SchweinDocumentMapper mapper;

    @Override
    public Mono<Void> save(Schwein schwein) {
        return springRepo.save(mapper.toDocument(schwein)).then();
    }

    @Override
    public Mono<Schwein> findById(UUID id) {
        return springRepo.findById(id.toString())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Schwein> findAll() {
        return springRepo.findAll()
                .map(mapper::toDomain);
    }
}
