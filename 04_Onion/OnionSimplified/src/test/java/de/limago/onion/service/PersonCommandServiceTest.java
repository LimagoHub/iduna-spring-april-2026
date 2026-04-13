package de.limago.onion.service;

import de.limago.onion.application.command.person.*;
import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.out.BankAccountRepository;
import de.limago.onion.application.port.out.DomainEventPublisher;
import de.limago.onion.application.port.out.PersonRepository;
import de.limago.onion.domain.event.*;
import de.limago.onion.domain.model.aggregate.BankAccountAggregate;
import de.limago.onion.domain.model.aggregate.PersonAggregate;
import de.limago.onion.domain.model.valueobject.Address;
import de.limago.onion.domain.model.valueobject.BankDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PersonCommandServiceTest {

    private FakePersonRepository repository;
    private FakeBankAccountRepository bankAccountRepository;
    private CapturingEventPublisher eventPublisher;
    private PersonCommandService service;

    private final Address berlin  = new Address("Hauptstraße", "1", "10115", "Berlin", "Deutschland");
    private final Address hamburg = new Address("Reeperbahn", "42", "20359", "Hamburg", "Deutschland");

    @BeforeEach
    void setUp() {
        repository = new FakePersonRepository();
        bankAccountRepository = new FakeBankAccountRepository();
        eventPublisher = new CapturingEventPublisher();
        service = new PersonCommandService(repository, bankAccountRepository, eventPublisher);
    }

    @Nested
    class CreatePerson {

        @Test
        void shouldReturnProvidedId() {
            UUID providedId = UUID.randomUUID();
            StepVerifier.create(service.createPerson(new CreatePersonCommand(providedId, "Max", "Muster", berlin)))
                    .assertNext(id -> assertThat(id).isEqualTo(providedId))
                    .verifyComplete();
        }

        @Test
        void shouldPersistPerson() {
            UUID[] savedId = new UUID[1];
            StepVerifier.create(service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)))
                    .assertNext(id -> savedId[0] = id)
                    .verifyComplete();

            assertThat(repository.findById(savedId[0]).block()).isNotNull();
        }

        @Test
        void shouldPublishPersonCreatedEvent() {
            StepVerifier.create(service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)))
                    .assertNext(id -> assertThat(eventPublisher.publishedEvents())
                            .hasSize(1).first().isInstanceOf(PersonCreatedEvent.class))
                    .verifyComplete();
        }
    }

    @Nested
    class MovePerson {

        @Test
        void shouldUpdateAddress() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.movePerson(new MovePersonCommand(id, hamburg)))
                    .verifyComplete();

            assertThat(repository.findById(id).block().getAddress()).isEqualTo(hamburg);
        }

        @Test
        void shouldPublishPersonMovedEvent() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.movePerson(new MovePersonCommand(id, hamburg)))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents())
                    .hasSize(1).first().isInstanceOf(PersonMovedEvent.class);
        }

        @Test
        void shouldErrorWithNotFoundExceptionForUnknownId() {
            StepVerifier.create(service.movePerson(new MovePersonCommand(UUID.randomUUID(), hamburg)))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class CorrectPerson {

        @Test
        void shouldUpdateFirstName() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .verifyComplete();

            assertThat(repository.findById(id).block().getFirstName()).isEqualTo("Moritz");
        }

        @Test
        void shouldUpdateLastName() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.of("Meier"), Optional.empty())))
                    .verifyComplete();

            assertThat(repository.findById(id).block().getLastName()).isEqualTo("Meier");
        }

        @Test
        void shouldUpdateAddress() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            assertThat(repository.findById(id).block().getAddress()).isEqualTo(hamburg);
        }

        @Test
        void shouldPublishPersonChangedEventWhenNameChanged() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents())
                    .hasSize(1).first().isInstanceOf(PersonChangedEvent.class);
        }

        @Test
        void shouldPublishAddressCorrectedEventWhenAddressChanged() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents())
                    .hasSize(1).first().isInstanceOf(PersonAddressCorrectedEvent.class);
        }

        @Test
        void shouldPublishBothEventsWhenNameAndAddressChanged() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents()).hasSize(2);
            assertThat(eventPublisher.publishedEvents()).anyMatch(e -> e instanceof PersonChangedEvent);
            assertThat(eventPublisher.publishedEvents()).anyMatch(e -> e instanceof PersonAddressCorrectedEvent);
        }

        @Test
        void shouldErrorWithNotFoundExceptionForUnknownId() {
            StepVerifier.create(service.correct(new CorrectPersonCommand(UUID.randomUUID(), Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class DeletePerson {

        @Test
        void shouldMarkPersonAsDeleted() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            assertThat(repository.findById(id).block().isDeleted()).isTrue();
        }

        @Test
        void shouldPublishPersonDeletedEvent() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            eventPublisher.clear();

            StepVerifier.create(service.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents())
                    .hasSize(1).first().isInstanceOf(PersonDeletedEvent.class);
        }

        @Test
        void shouldCascadeDeleteBankAccount() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            bankAccountRepository.save(BankAccountAggregate.create(id,
                    new BankDetails("DE89370400440532013000", "COBADEFFXXX", "Commerzbank"))).block();

            StepVerifier.create(service.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            assertThat(bankAccountRepository.findByPersonId(id).block()).isNull();
        }

        @Test
        void shouldBeIdempotentWhenAlreadyDeleted() {
            UUID id = service.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)).block();
            service.deletePerson(new DeletePersonCommand(id)).block();
            eventPublisher.clear();

            StepVerifier.create(service.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            assertThat(eventPublisher.publishedEvents()).isEmpty();
        }
    }

    // --- Test Doubles ---

    static class FakeBankAccountRepository implements BankAccountRepository {
        private final Map<UUID, BankAccountAggregate> store = new HashMap<>();

        @Override public Mono<Void> save(BankAccountAggregate a) { store.put(a.getPersonId(), a); return Mono.empty(); }
        @Override public Mono<BankAccountAggregate> findByPersonId(UUID id) { return Mono.justOrEmpty(store.get(id)); }
        @Override public Mono<Void> deleteByPersonId(UUID id) { store.remove(id); return Mono.empty(); }
    }

    static class FakePersonRepository implements PersonRepository {
        private final Map<UUID, PersonAggregate> store = new HashMap<>();

        @Override public Mono<Void> save(PersonAggregate p) { store.put(p.getId(), p); return Mono.empty(); }
        @Override public Mono<PersonAggregate> findById(UUID id) { return Mono.justOrEmpty(store.get(id)); }
        @Override public reactor.core.publisher.Flux<PersonAggregate> findAll() { return reactor.core.publisher.Flux.fromIterable(store.values()); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        private final List<Object> events = new ArrayList<>();

        @Override public Mono<Void> publish(List<Object> newEvents) { events.addAll(newEvents); return Mono.empty(); }
        List<Object> publishedEvents() { return Collections.unmodifiableList(events); }
        void clear() { events.clear(); }
    }
}
