package de.limago.onion.application.commandside.person.port.inport.person;

import de.limago.onion.application.commandside.person.command.person.MovePersonCommand;
import reactor.core.publisher.Mono;

public interface MovePersonUseCase {
    Mono<Void> movePerson(MovePersonCommand command);
}
