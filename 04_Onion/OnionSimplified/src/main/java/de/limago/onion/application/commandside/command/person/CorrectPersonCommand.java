package de.limago.onion.application.commandside.command.person;

import de.limago.onion.domain.model.valueobject.Address;

import java.util.Optional;
import java.util.UUID;

public record CorrectPersonCommand(
        UUID personId,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<Address> address
) {}
