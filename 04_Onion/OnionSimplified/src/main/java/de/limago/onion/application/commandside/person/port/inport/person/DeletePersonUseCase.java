package de.limago.onion.application.commandside.person.port.inport.person;

import de.limago.onion.application.commandside.person.command.person.DeletePersonCommand;
import reactor.core.publisher.Mono;

public interface DeletePersonUseCase {
    Mono<Void> deletePerson(DeletePersonCommand command);
}
