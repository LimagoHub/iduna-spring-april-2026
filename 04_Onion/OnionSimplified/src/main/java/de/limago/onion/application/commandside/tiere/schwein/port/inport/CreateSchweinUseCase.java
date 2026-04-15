package de.limago.onion.application.commandside.tiere.schwein.port.inport;

import de.limago.onion.application.commandside.tiere.schwein.command.CreateSchweinCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreateSchweinUseCase {
    Mono<UUID> createSchwein(CreateSchweinCommand command);
}
