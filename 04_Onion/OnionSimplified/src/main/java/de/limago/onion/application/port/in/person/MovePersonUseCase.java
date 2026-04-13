package de.limago.onion.application.port.in.person;

import de.limago.onion.application.command.person.MovePersonCommand;
import reactor.core.publisher.Mono;

public interface MovePersonUseCase {
    Mono<Void> movePerson(MovePersonCommand command);
}
