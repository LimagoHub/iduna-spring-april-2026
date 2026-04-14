package de.limago.onion.application.port.inport.person;

import de.limago.onion.application.commandside.command.person.MovePersonCommand;
import reactor.core.publisher.Mono;

public interface MovePersonUseCase {
    Mono<Void> movePerson(MovePersonCommand command);
}
