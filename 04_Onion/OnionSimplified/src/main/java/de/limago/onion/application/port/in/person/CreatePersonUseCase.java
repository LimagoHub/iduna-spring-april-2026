package de.limago.onion.application.port.in.person;

import de.limago.onion.application.command.person.CreatePersonCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreatePersonUseCase {
    Mono<UUID> createPerson(CreatePersonCommand command);
}
