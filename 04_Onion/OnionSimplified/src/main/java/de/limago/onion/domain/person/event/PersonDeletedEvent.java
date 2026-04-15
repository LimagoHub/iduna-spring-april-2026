package de.limago.onion.domain.person.event;

import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PersonDeletedEvent(
        UUID personId,
        Instant deletedAt,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static PersonDeletedEvent of(UUID personId, Instant deletedAt) {
        return new PersonDeletedEvent(personId, deletedAt, UUID.randomUUID(), Instant.now());
    }
}
