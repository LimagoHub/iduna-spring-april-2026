package de.limago.onion.application.command.person;

import de.limago.onion.domain.model.valueobject.Address;

import java.util.UUID;

public record MovePersonCommand(
        UUID personId,
        Address newAddress
) {}
