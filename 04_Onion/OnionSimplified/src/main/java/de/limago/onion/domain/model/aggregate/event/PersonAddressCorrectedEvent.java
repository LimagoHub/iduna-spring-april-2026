package de.limago.onion.domain.model.aggregate.event;

import de.limago.onion.domain.model.valueobject.Address;

import java.time.Instant;
import java.util.UUID;

/**
 * Ausgelöst bei Korrektur einer fehlerhaft erfassten Adresse (z.B. Tippfehler).
 * Fachlich unterschiedlich von PersonMovedEvent.
 */
public record PersonAddressCorrectedEvent(
        UUID personId,
        Address oldAddress,
        Address correctedAddress,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static PersonAddressCorrectedEvent of(UUID personId, Address oldAddress, Address correctedAddress) {
        return new PersonAddressCorrectedEvent(personId, oldAddress, correctedAddress, UUID.randomUUID(), Instant.now());
    }
}
