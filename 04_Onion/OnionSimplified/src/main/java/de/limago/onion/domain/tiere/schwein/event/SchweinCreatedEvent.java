package de.limago.onion.domain.tiere.schwein.event;


import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SchweinCreatedEvent(
        UUID personId,
        String name,
        int gewicht,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static SchweinCreatedEvent of(UUID personId, String name, int gewicht) {
        return new SchweinCreatedEvent(personId, name, gewicht, UUID.randomUUID(), Instant.now());
    }
}
