package de.limago.onion.domain.person.event;

import de.limago.onion.domain.person.valueobject.BankDetails;
import de.limago.onion.domain.shared.DomainEvent;

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
