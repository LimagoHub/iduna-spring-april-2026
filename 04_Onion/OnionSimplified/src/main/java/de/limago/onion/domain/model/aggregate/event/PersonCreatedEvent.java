package de.limago.onion.domain.model.aggregate.event;

import de.limago.onion.domain.model.valueobject.Address;

import java.time.Instant;
import java.util.UUID;

public record PersonCreatedEvent(
        UUID personId,
        String firstName,
        String lastName,
        Address address,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static PersonCreatedEvent of(UUID personId, String firstName, String lastName, Address address) {
        return new PersonCreatedEvent(personId, firstName, lastName, address, UUID.randomUUID(), Instant.now());
    }
}
