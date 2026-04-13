package de.limago.onion.application.query.person;

import de.limago.onion.domain.model.valueobject.Address;

import java.util.UUID;

public record PersonResult(
        UUID id,
        String firstName,
        String lastName,
        Address address
) {}
