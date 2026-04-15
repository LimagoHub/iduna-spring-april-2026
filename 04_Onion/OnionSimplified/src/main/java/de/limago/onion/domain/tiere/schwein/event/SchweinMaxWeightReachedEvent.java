package de.limago.onion.domain.tiere.schwein.event;

import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SchweinMaxWeightReachedEvent(
        UUID personId,
        String name,
        int gewicht,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {
    public static SchweinMaxWeightReachedEvent of(UUID personId, String name, int gewicht) {
        return new SchweinMaxWeightReachedEvent(personId, name, gewicht, UUID.randomUUID(), Instant.now());
    }
}
