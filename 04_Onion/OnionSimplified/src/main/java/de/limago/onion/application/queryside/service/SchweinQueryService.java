package de.limago.onion.application.queryside.service;

import de.limago.onion.application.commandside.tiere.schwein.port.inport.FindSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.outport.SchweinRepository;
import de.limago.onion.application.queryside.query.schwein.FindSchweinByIdQuery;
import de.limago.onion.application.queryside.query.schwein.SchweinResult;
import de.limago.onion.application.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchweinQueryService implements FindSchweinUseCase {

    private final SchweinRepository schweinRepository;

    @Override
    public Mono<SchweinResult> findById(FindSchweinByIdQuery query) {
        return schweinRepository.findById(query.schweinId())
                .switchIfEmpty(Mono.error(new NotFoundException("Schwein", query.schweinId())))
                .map(schwein -> new SchweinResult(schwein.getId(), schwein.getName(), schwein.getGewicht()));
    }

    @Override
    public Flux<SchweinResult> findAll() {
        return schweinRepository.findAll()
                .map(schwein -> new SchweinResult(schwein.getId(), schwein.getName(), schwein.getGewicht()));
    }
}
