package de.limago.onion.application.commandside.person.command.person;

import de.limago.onion.domain.person.valueobject.Address;

import java.util.Optional;
import java.util.UUID;

public record CorrectPersonCommand(
        UUID personId,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<Address> address
) {}
