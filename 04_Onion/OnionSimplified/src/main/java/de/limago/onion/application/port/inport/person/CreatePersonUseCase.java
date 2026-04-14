package de.limago.onion.application.port.inport.person;

import de.limago.onion.application.commandside.command.person.CreatePersonCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreatePersonUseCase {
    Mono<UUID> createPerson(CreatePersonCommand command);
}
