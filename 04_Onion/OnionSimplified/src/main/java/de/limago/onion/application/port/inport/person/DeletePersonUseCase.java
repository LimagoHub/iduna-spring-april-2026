package de.limago.onion.application.port.inport.person;

import de.limago.onion.application.commandside.command.person.DeletePersonCommand;
import reactor.core.publisher.Mono;

public interface DeletePersonUseCase {
    Mono<Void> deletePerson(DeletePersonCommand command);
}
