package de.limago.onion.application.commandside.person.command.person;

import de.limago.onion.domain.person.valueobject.Address;

import java.util.UUID;

public record MovePersonCommand(
        UUID personId,
        Address newAddress
) {}
