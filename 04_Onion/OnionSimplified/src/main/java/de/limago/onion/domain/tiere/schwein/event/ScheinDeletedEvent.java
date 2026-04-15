package de.limago.onion.domain.tiere.schwein.event;

import java.time.Instant;
import java.util.UUID;

public record ScheinDeletedEvent(
        UUID personId,
        String name,
        int gewicht,
        UUID eventId,
        Instant occurredAt
) {
    public static ScheinDeletedEvent of(UUID personId, String name, int gewicht) {
        return new ScheinDeletedEvent(personId, name, gewicht, UUID.randomUUID(), Instant.now());
    }
}
