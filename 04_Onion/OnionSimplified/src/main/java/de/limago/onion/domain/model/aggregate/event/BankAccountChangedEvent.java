package de.limago.onion.domain.model.aggregate.event;

import de.limago.onion.domain.model.valueobject.BankDetails;

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
