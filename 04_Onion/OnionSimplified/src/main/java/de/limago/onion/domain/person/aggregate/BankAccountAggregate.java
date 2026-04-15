package de.limago.onion.domain.person.aggregate;


import de.limago.onion.domain.person.event.BankAccountChangedEvent;
import de.limago.onion.domain.person.event.BankAccountCreatedEvent;
import de.limago.onion.domain.person.valueobject.BankDetails;
import de.limago.onion.domain.shared.AggregateRoot;
import lombok.Getter;

import java.util.UUID;

@Getter
public class BankAccountAggregate extends AggregateRoot {

    private BankDetails bankDetails;

    private BankAccountAggregate(UUID personId, BankDetails bankDetails) {
        super(personId);
        this.bankDetails = bankDetails;
    }

    public UUID getPersonId() {
        return getId();
    }

    public static BankAccountAggregate create(UUID personId, BankDetails bankDetails) {
        BankAccountAggregate account = new BankAccountAggregate(personId, bankDetails);
        account.registerEvent(BankAccountCreatedEvent.of(personId, bankDetails));
        return account;
    }

    public static BankAccountAggregate reconstitute(UUID personId, Long version, BankDetails bankDetails) {
        BankAccountAggregate account = new BankAccountAggregate(personId, bankDetails);
        account.setVersion(version);
        return account;
    }

    public void changeBankDetails(BankDetails newDetails) {
        BankDetails old = this.bankDetails;
        this.bankDetails = newDetails;
        registerEvent(BankAccountChangedEvent.of(getId(), old, newDetails));
    }
}
