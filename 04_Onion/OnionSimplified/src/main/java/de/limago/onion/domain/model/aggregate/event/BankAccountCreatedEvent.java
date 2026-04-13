package de.limago.onion.domain.model.aggregate.event;

import de.limago.onion.domain.model.valueobject.BankDetails;

import java.time.Instant;
import java.util.UUID;

public record BankAccountCreatedEvent(
        UUID personId,
        BankDetails bankDetails,
        UUID eventId,
        Instant occurredAt
) implements DomainEvent {

    public static BankAccountCreatedEvent of(UUID personId, BankDetails bankDetails) {
        return new BankAccountCreatedEvent(personId, bankDetails, UUID.randomUUID(), Instant.now());
    }
}
