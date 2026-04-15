package de.limago.onion.domain.person.event;

import de.limago.onion.domain.person.valueobject.Address;
import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Ausgelöst wenn eine Person umzieht – bewusst von PersonAddressCorrectedEvent getrennt,
 * da "Umzug" fachlich etwas anderes bedeutet als eine Korrektur (z.B. Tippfehler).
 */
public record PersonMovedEvent(
        UUID personId,
        Address oldAddress,
        Address newAddress,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static PersonMovedEvent of(UUID personId, Address oldAddress, Address newAddress) {
        return new PersonMovedEvent(personId, oldAddress, newAddress, UUID.randomUUID(), Instant.now());
    }
}
