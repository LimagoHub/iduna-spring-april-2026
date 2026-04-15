package de.limago.onion.application.commandside.tiere.schwein.port.inport;

import de.limago.onion.application.commandside.tiere.schwein.command.UpdateSchweinCommand;
import reactor.core.publisher.Mono;

public interface UpdateSchweinUseCase {
    Mono<Void> updateSchwein(UpdateSchweinCommand command);
}
