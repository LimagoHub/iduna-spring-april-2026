package de.limago.onion.application.commandside.tiere.schwein.command;

import de.limago.onion.domain.person.valueobject.Address;

import java.util.Optional;
import java.util.UUID;

public record UpdateSchweinCommand(
        UUID schweinId,
        Optional<String> name,
        Optional<Integer> gewicht

) {}
