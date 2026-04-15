package de.limago.onion.domain.person.aggregate;


import de.limago.onion.domain.person.event.*;
import de.limago.onion.domain.person.valueobject.Address;
import de.limago.onion.domain.shared.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Getter
public class PersonAggregate extends AggregateRoot {

    private String firstName;
    private String lastName;
    private Address address;
    private Instant deletedAt;

    private PersonAggregate(UUID id, String firstName, String lastName, Address address) {
        super(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    public static PersonAggregate create(UUID id, String firstName, String lastName, Address address) {
        PersonAggregate person = new PersonAggregate(id, firstName, lastName, address);
        person.registerEvent(PersonCreatedEvent.of(person.getId(), firstName, lastName, address));
        return person;
    }

    public static PersonAggregate reconstitute(UUID id, Long version, String firstName, String lastName,
                                               Address address, Instant deletedAt) {
        PersonAggregate person = new PersonAggregate(id, firstName, lastName, address);
        person.setVersion(version);
        person.deletedAt = deletedAt;
        return person;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(PersonDeletedEvent.of(getId(), this.deletedAt));
    }

    public void correctPerson(Optional<String> newFirstName, Optional<String> newLastName) {
        newFirstName.ifPresent(n -> this.firstName = n);
        newLastName.ifPresent(n -> this.lastName = n);
        registerEvent(PersonChangedEvent.of(getId(), newFirstName, newLastName));
    }

    /**
     * Umzug: Die Person zieht an eine neue Adresse.
     * Fachlich bewusst getrennt von correctAddress().
     */
    public void moveTo(Address newAddress) {
        Address old = this.address;
        this.address = newAddress;
        registerEvent(PersonMovedEvent.of(getId(), old, newAddress));
    }

    /**
     * Adresskorrektur: Ein Fehler bei der Erfassung wird berichtigt.
     */
    public void correctAddress(Address correctedAddress) {
        Address old = this.address;
        this.address = correctedAddress;
        registerEvent(PersonAddressCorrectedEvent.of(getId(), old, correctedAddress));
    }
}
