package de.limago.onion.application.commandside.tiere.schwein.port.inport;

import de.limago.onion.application.commandside.tiere.schwein.command.DeleteSchweinCommand;
import reactor.core.publisher.Mono;

public interface DeleteSchweinUseCase {
    Mono<Void> deleteSchwein(DeleteSchweinCommand command);
}
