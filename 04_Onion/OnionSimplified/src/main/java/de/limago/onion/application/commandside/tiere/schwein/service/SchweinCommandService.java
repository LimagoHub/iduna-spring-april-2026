package de.limago.onion.application.commandside.tiere.schwein.service;

import de.limago.onion.application.commandside.tiere.schwein.command.CreateSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.DeleteSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.FuetternSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.UpdateSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.exception.SchweinServiceException;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.CreateSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.DeleteSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.FuetternSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.UpdateSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.outport.SchweinRepository;
import de.limago.onion.application.shared.AlreadyExistsException;
import de.limago.onion.application.shared.DomainEventPublisher;
import de.limago.onion.application.shared.NotFoundException;
import de.limago.onion.domain.tiere.schwein.aggregate.Schwein;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Transactional
public class SchweinCommandService implements
        CreateSchweinUseCase,
        UpdateSchweinUseCase,
        DeleteSchweinUseCase,
        FuetternSchweinUseCase {

    private final SchweinRepository schweinRepository;
    private final DomainEventPublisher eventPublisher;

    private static final Predicate<Throwable> IS_TECHNICAL =
            e -> !(e instanceof NotFoundException) && !(e instanceof AlreadyExistsException);

    @Override
    @PreAuthorize("hasRole('BAUER')")
    public Mono<UUID> createSchwein(CreateSchweinCommand command) {
        return schweinRepository.findById(command.schweinId())
                .flatMap(existing -> Mono.<UUID>error(new AlreadyExistsException("Schwein", command.schweinId())))
                .switchIfEmpty(Mono.defer(() -> {
                    Schwein schwein = Schwein.create(command.schweinId(), command.name());
                    return saveAndPublish(schwein)
                            .thenReturn(command.schweinId());
                }))
                .onErrorMap(IS_TECHNICAL, SchweinServiceException::new);
    }

    @Override
    @PreAuthorize("hasRole('BAUER')")
    public Mono<Void> updateSchwein(UpdateSchweinCommand command) {
        return loadSchwein(command.schweinId())
                .flatMap(schwein -> {
                    command.name().ifPresent(schwein::taufen);
                    return saveAndPublish(schwein);
                })
                .onErrorMap(IS_TECHNICAL, SchweinServiceException::new);
    }

    @Override
    @PreAuthorize("hasRole('BAUER')")
    public Mono<Void> deleteSchwein(DeleteSchweinCommand command) {
        return loadSchwein(command.schwein())
                .flatMap(schwein -> {
                    schwein.markAsDeleted();
                    return saveAndPublish(schwein);
                })
                .onErrorMap(IS_TECHNICAL, SchweinServiceException::new);
    }

    @Override
    @PreAuthorize("hasAnyRole('BAUER', 'VORGESETZTER')")
    public Mono<Void> fuettern(FuetternSchweinCommand command) {
        return loadSchwein(command.schweinId())
                .flatMap(schwein -> {
                    schwein.fuettern();
                    return saveAndPublish(schwein);
                })
                .onErrorMap(IS_TECHNICAL, SchweinServiceException::new);
    }

    private Mono<Schwein> loadSchwein(UUID id) {
        return schweinRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Schwein", id)));
    }

    private Mono<Void> saveAndPublish(Schwein schwein) {
        return schweinRepository.save(schwein)
                .then(eventPublisher.publish(schwein.getDomainEvents()))
                .then(Mono.<Void>fromRunnable(schwein::clearDomainEvents));
    }
}
