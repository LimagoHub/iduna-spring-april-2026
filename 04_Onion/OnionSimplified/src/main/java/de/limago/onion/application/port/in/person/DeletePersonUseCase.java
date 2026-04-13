package de.limago.onion.application.port.in.person;

import de.limago.onion.application.command.person.DeletePersonCommand;
import reactor.core.publisher.Mono;

public interface DeletePersonUseCase {
    Mono<Void> deletePerson(DeletePersonCommand command);
}
