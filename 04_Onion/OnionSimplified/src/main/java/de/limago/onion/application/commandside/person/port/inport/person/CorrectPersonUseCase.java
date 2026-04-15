package de.limago.onion.application.commandside.person.port.inport.person;

import de.limago.onion.application.commandside.person.command.person.CorrectPersonCommand;
import reactor.core.publisher.Mono;

public interface CorrectPersonUseCase {
    Mono<Void> correct(CorrectPersonCommand command);
}
