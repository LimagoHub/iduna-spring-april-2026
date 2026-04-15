package de.limago.onion.application.commandside.tiere.schwein.command;

import de.limago.onion.domain.person.valueobject.Address;

import java.util.UUID;

public record CreateSchweinCommand(
        UUID schweinId,
        String name,
        int gewicht
) {}
