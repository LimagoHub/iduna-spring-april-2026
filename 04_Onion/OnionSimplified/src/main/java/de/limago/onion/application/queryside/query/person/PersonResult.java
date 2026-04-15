package de.limago.onion.application.queryside.query.person;

import de.limago.onion.domain.person.valueobject.Address;

import java.util.UUID;

public record PersonResult(
        UUID id,
        String firstName,
        String lastName,
        Address address
) {}
