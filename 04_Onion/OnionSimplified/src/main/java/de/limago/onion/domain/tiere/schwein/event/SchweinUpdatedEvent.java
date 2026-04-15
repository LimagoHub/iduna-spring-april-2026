package de.limago.onion.domain.tiere.schwein.event;

import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SchweinUpdatedEvent(UUID personId,
                                  String name,
                                  int gewicht,
                                  UUID eventId,
                                  Instant occurredAt)implements DomainEvent {
    public static SchweinUpdatedEvent of(UUID personId, String name, int gewicht) {
        return new SchweinUpdatedEvent(personId, name, gewicht, UUID.randomUUID(), Instant.now());
    }
}
