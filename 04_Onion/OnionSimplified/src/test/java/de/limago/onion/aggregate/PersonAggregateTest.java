package de.limago.onion.aggregate;


import de.limago.onion.domain.model.aggregate.PersonAggregate;
import de.limago.onion.domain.model.aggregate.event.*;
import de.limago.onion.domain.model.valueobject.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersonAggregateTest {

    private Address berlinAddress;
    private Address hamburgAddress;

    @BeforeEach
    void setUp() {
        berlinAddress = new Address("Hauptstraße", "1", "10115", "Berlin", "Deutschland");
        hamburgAddress = new Address("Reeperbahn", "42", "20359", "Hamburg", "Deutschland");
    }

    @Nested
    class Create {

        @Test
        void shouldSetAllFields() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.create(id, "Max", "Muster", berlinAddress);

            assertThat(person.getId()).isEqualTo(id);
            assertThat(person.getFirstName()).isEqualTo("Max");
            assertThat(person.getLastName()).isEqualTo("Muster");
            assertThat(person.getAddress()).isEqualTo(berlinAddress);
        }

        @Test
        void shouldUseProvidedId() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.create(id, "Max", "Muster", berlinAddress);

            assertThat(person.getId()).isEqualTo(id);
        }

        @Test
        void shouldRegisterPersonCreatedEvent() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.create(id, "Max", "Muster", berlinAddress);

            assertThat(person.getDomainEvents())
                    .hasSize(1)
                    .first().isInstanceOf(PersonCreatedEvent.class);

            PersonCreatedEvent event = (PersonCreatedEvent) person.getDomainEvents().getFirst();
            assertThat(event.personId()).isEqualTo(id);
            assertThat(event.firstName()).isEqualTo("Max");
            assertThat(event.lastName()).isEqualTo("Muster");
            assertThat(event.address()).isEqualTo(berlinAddress);
        }
    }

    @Nested
    class Reconstitute {

        @Test
        void shouldRestoreAllFields() {
            UUID existingId = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(existingId, 3L, "Max", "Muster", berlinAddress, null);

            assertThat(person.getId()).isEqualTo(existingId);
            assertThat(person.getFirstName()).isEqualTo("Max");
            assertThat(person.getLastName()).isEqualTo("Muster");
            assertThat(person.getAddress()).isEqualTo(berlinAddress);
        }

        @Test
        void shouldRestoreVersion() {
            PersonAggregate person = PersonAggregate.reconstitute(UUID.randomUUID(), 5L, "Max", "Muster", berlinAddress, null);
            assertThat(person.getVersion()).isEqualTo(5L);
        }

        @Test
        void shouldNotRegisterAnyEvents() {
            PersonAggregate person = PersonAggregate.reconstitute(UUID.randomUUID(), 0L, "Max", "Muster", berlinAddress, null);
            assertThat(person.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    class MoveTo {

        @Test
        void shouldUpdateAddress() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.moveTo(hamburgAddress);

            assertThat(person.getAddress()).isEqualTo(hamburgAddress);
        }

        @Test
        void shouldRegisterPersonMovedEvent() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.moveTo(hamburgAddress);

            assertThat(person.getDomainEvents()).hasSize(1);
            PersonMovedEvent event = (PersonMovedEvent) person.getDomainEvents().getFirst();
            assertThat(event.oldAddress()).isEqualTo(berlinAddress);
            assertThat(event.newAddress()).isEqualTo(hamburgAddress);
        }
    }

    @Nested
    class CorrectAddress {

        @Test
        void shouldUpdateAddress() {
            Address typo = new Address("Hauptstrase", "1", "10115", "Berlin", "Deutschland");
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", typo);
            person.clearDomainEvents();

            person.correctAddress(berlinAddress);

            assertThat(person.getAddress()).isEqualTo(berlinAddress);
        }

        @Test
        void shouldRegisterPersonAddressCorrectedEvent() {
            Address typo = new Address("Hauptstrase", "1", "10115", "Berlin", "Deutschland");
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", typo);
            person.clearDomainEvents();

            person.correctAddress(berlinAddress);

            assertThat(person.getDomainEvents()).hasSize(1);
            PersonAddressCorrectedEvent event = (PersonAddressCorrectedEvent) person.getDomainEvents().getFirst();
            assertThat(event.oldAddress()).isEqualTo(typo);
            assertThat(event.correctedAddress()).isEqualTo(berlinAddress);
        }

        @Test
        void shouldProduceDifferentEventThanMoveTo() {
            PersonAggregate personMoved = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            personMoved.clearDomainEvents();
            personMoved.moveTo(hamburgAddress);

            PersonAggregate personCorrected = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            personCorrected.clearDomainEvents();
            personCorrected.correctAddress(hamburgAddress);

            assertThat(personMoved.getDomainEvents().getFirst()).isInstanceOf(PersonMovedEvent.class);
            assertThat(personCorrected.getDomainEvents().getFirst()).isInstanceOf(PersonAddressCorrectedEvent.class);
        }
    }

    @Nested
    class CorrectPerson {

        @Test
        void shouldUpdateFirstName() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.correctPerson(Optional.of("Moritz"), Optional.empty());

            assertThat(person.getFirstName()).isEqualTo("Moritz");
            assertThat(person.getLastName()).isEqualTo("Muster");
        }

        @Test
        void shouldUpdateLastName() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.correctPerson(Optional.empty(), Optional.of("Meier"));

            assertThat(person.getLastName()).isEqualTo("Meier");
            assertThat(person.getFirstName()).isEqualTo("Max");
        }

        @Test
        void shouldRegisterPersonChangedEvent() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.correctPerson(Optional.of("Moritz"), Optional.of("Meier"));

            assertThat(person.getDomainEvents()).hasSize(1);
            PersonChangedEvent event = (PersonChangedEvent) person.getDomainEvents().getFirst();
            assertThat(event.newFirstName()).contains("Moritz");
            assertThat(event.newLastName()).contains("Meier");
        }
    }

    @Nested
    class MarkAsDeleted {

        @Test
        void shouldSetDeletedAt() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            Instant before = Instant.now();
            person.markAsDeleted();
            Instant after = Instant.now();

            assertThat(person.isDeleted()).isTrue();
            assertThat(person.getDeletedAt()).isBetween(before, after);
        }

        @Test
        void shouldRegisterPersonDeletedEvent() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();

            person.markAsDeleted();

            assertThat(person.getDomainEvents()).hasSize(1);
            PersonDeletedEvent event = (PersonDeletedEvent) person.getDomainEvents().getFirst();
            assertThat(event.personId()).isEqualTo(person.getId());
        }

        @Test
        void shouldBeIdempotent() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.clearDomainEvents();
            person.markAsDeleted();
            Instant firstDeletedAt = person.getDeletedAt();
            person.clearDomainEvents();

            person.markAsDeleted();

            assertThat(person.getDomainEvents()).isEmpty();
            assertThat(person.getDeletedAt()).isEqualTo(firstDeletedAt);
        }
    }

    @Nested
    class DomainEvents {

        @Test
        void shouldAccumulateMultipleEvents() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            person.moveTo(hamburgAddress);
            person.correctPerson(Optional.empty(), Optional.of("Meier"));

            assertThat(person.getDomainEvents()).hasSize(3);
            assertThat(person.getDomainEvents().get(0)).isInstanceOf(PersonCreatedEvent.class);
            assertThat(person.getDomainEvents().get(1)).isInstanceOf(PersonMovedEvent.class);
            assertThat(person.getDomainEvents().get(2)).isInstanceOf(PersonChangedEvent.class);
        }

        @Test
        void shouldClearEventsAfterProcessing() {
            PersonAggregate person = PersonAggregate.create(UUID.randomUUID(), "Max", "Muster", berlinAddress);
            assertThat(person.getDomainEvents()).isNotEmpty();

            person.clearDomainEvents();

            assertThat(person.getDomainEvents()).isEmpty();
        }
    }
}
