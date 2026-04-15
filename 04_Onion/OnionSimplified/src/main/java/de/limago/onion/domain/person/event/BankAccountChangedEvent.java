package de.limago.onion.domain.person.event;

import de.limago.onion.domain.person.valueobject.BankDetails;
import de.limago.onion.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record BankAccountChangedEvent(
        UUID personId,
        BankDetails oldDetails,
        BankDetails newDetails,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static BankAccountChangedEvent of(UUID personId, BankDetails oldDetails, BankDetails newDetails) {
        return new BankAccountChangedEvent(personId, oldDetails, newDetails, UUID.randomUUID(), Instant.now());
    }
}
