package de.limago.onion.application.commandside.tiere.schwein.port.inport;

import de.limago.onion.application.commandside.tiere.schwein.command.FuetternSchweinCommand;
import reactor.core.publisher.Mono;

public interface FuetternSchweinUseCase {
    Mono<Void> fuettern(FuetternSchweinCommand command);
}
