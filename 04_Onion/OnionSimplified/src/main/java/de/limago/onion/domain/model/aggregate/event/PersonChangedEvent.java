package de.limago.onion.domain.model.aggregate.event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PersonChangedEvent(
        UUID personId,
        Optional<String> newFirstName,
        Optional<String> newLastName,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static PersonChangedEvent of(UUID personId, Optional<String> newFirstName, Optional<String> newLastName) {
        return new PersonChangedEvent(personId, newFirstName, newLastName, UUID.randomUUID(), Instant.now());
    }
}
