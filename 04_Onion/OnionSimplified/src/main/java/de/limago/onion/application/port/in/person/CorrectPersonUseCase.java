package de.limago.onion.application.port.in.person;

import de.limago.onion.application.command.person.CorrectPersonCommand;
import reactor.core.publisher.Mono;

public interface CorrectPersonUseCase {
    Mono<Void> correct(CorrectPersonCommand command);
}
