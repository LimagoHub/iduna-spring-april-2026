package de.limago.onion.application.commandside.service;

import de.limago.onion.application.commandside.person.command.person.CorrectPersonCommand;
import de.limago.onion.application.commandside.person.command.person.CreatePersonCommand;
import de.limago.onion.application.commandside.person.command.person.DeletePersonCommand;
import de.limago.onion.application.commandside.person.command.person.MovePersonCommand;
import de.limago.onion.application.commandside.person.service.PersonCommandService;
import de.limago.onion.application.shared.NotFoundException;
import de.limago.onion.application.commandside.person.port.outport.BankAccountRepository;
import de.limago.onion.application.shared.DomainEventPublisher;
import de.limago.onion.application.commandside.person.port.outport.PersonRepository;
import de.limago.onion.domain.person.aggregate.PersonAggregate;
import de.limago.onion.domain.person.event.*;
import de.limago.onion.domain.person.valueobject.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonCommandServiceTest {

    @Mock private PersonRepository repositoryMock;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private DomainEventPublisher eventPublisher;
    @InjectMocks private PersonCommandService objectUnderTest;

    private final Address berlin  = new Address("Hauptstraße", "1", "10115", "Berlin", "Deutschland");
    private final Address hamburg = new Address("Reeperbahn", "42", "20359", "Hamburg", "Deutschland");

    @BeforeEach
    void setUp() {
        lenient().when(repositoryMock.findById(any())).thenReturn(Mono.empty());
        lenient().when(repositoryMock.save(any())).thenReturn(Mono.empty());
        lenient().when(eventPublisher.publish(any())).thenReturn(Mono.empty());
        lenient().when(bankAccountRepository.deleteByPersonId(any())).thenReturn(Mono.empty());
    }

    @Nested
    class CreatePerson {

        @Test
        void shouldReturnProvidedId() {
            UUID providedId = UUID.randomUUID();
            StepVerifier.create(objectUnderTest.createPerson(new CreatePersonCommand(providedId, "Max", "Muster", berlin)))
                    .assertNext(id -> assertThat(id).isEqualTo(providedId))
                    .verifyComplete();
        }

        @Test
        void shouldPersistPerson() {
            UUID id = UUID.randomUUID();
            StepVerifier.create(objectUnderTest.createPerson(new CreatePersonCommand(id, "Max", "Muster", berlin)))
                    .assertNext(returnedId -> assertThat(returnedId).isEqualTo(id))
                    .verifyComplete();

            verify(repositoryMock).save(argThat(p -> p.getId().equals(id)));
        }

        @Test
        void shouldPublishPersonCreatedEvent() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.createPerson(new CreatePersonCommand(UUID.randomUUID(), "Max", "Muster", berlin)))
                    .assertNext(id -> {})
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(PersonCreatedEvent.class);
        }
    }

    @Nested
    class MovePerson {

        @Test
        void shouldUpdateAddress() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.movePerson(new MovePersonCommand(id, hamburg)))
                    .verifyComplete();

            assertThat(person.getAddress()).isEqualTo(hamburg);
        }

        @Test
        void shouldPublishPersonMovedEvent() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.movePerson(new MovePersonCommand(id, hamburg)))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(PersonMovedEvent.class);
        }

        @Test
        void shouldErrorWithNotFoundExceptionForUnknownId() {
            StepVerifier.create(objectUnderTest.movePerson(new MovePersonCommand(UUID.randomUUID(), hamburg)))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class CorrectPerson {

        @Test
        void shouldUpdateFirstName() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .verifyComplete();

            assertThat(person.getFirstName()).isEqualTo("Moritz");
        }

        @Test
        void shouldUpdateLastName() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.of("Meier"), Optional.empty())))
                    .verifyComplete();

            assertThat(person.getLastName()).isEqualTo("Meier");
        }

        @Test
        void shouldUpdateAddress() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            assertThat(person.getAddress()).isEqualTo(hamburg);
        }

        @Test
        void shouldPublishPersonChangedEventWhenNameChanged() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(PersonChangedEvent.class);
        }

        @Test
        void shouldPublishAddressCorrectedEventWhenAddressChanged() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.empty(), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(PersonAddressCorrectedEvent.class);
        }

        @Test
        void shouldPublishBothEventsWhenNameAndAddressChanged() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(id, Optional.of("Moritz"), Optional.empty(), Optional.of(hamburg))))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue()).anyMatch(e -> e instanceof PersonChangedEvent);
            assertThat(captor.getValue()).anyMatch(e -> e instanceof PersonAddressCorrectedEvent);
        }

        @Test
        void shouldErrorWithNotFoundExceptionForUnknownId() {
            StepVerifier.create(objectUnderTest.correct(new CorrectPersonCommand(UUID.randomUUID(), Optional.of("Moritz"), Optional.empty(), Optional.empty())))
                    .expectError(NotFoundException.class)
                    .verify();
        }
    }

    @Nested
    class DeletePerson {

        @Test
        void shouldMarkPersonAsDeleted() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            assertThat(person.isDeleted()).isTrue();
        }

        @Test
        void shouldPublishPersonDeletedEvent() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).hasSize(1).first().isInstanceOf(PersonDeletedEvent.class);
        }

        @Test
        void shouldCascadeDeleteBankAccount() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, null);
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            StepVerifier.create(objectUnderTest.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            verify(bankAccountRepository).deleteByPersonId(id);
        }

        @Test
        void shouldBeIdempotentWhenAlreadyDeleted() {
            UUID id = UUID.randomUUID();
            PersonAggregate person = PersonAggregate.reconstitute(id, 0L, "Max", "Muster", berlin, Instant.now());
            when(repositoryMock.findById(id)).thenReturn(Mono.just(person));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Object>> captor = ArgumentCaptor.forClass(List.class);

            StepVerifier.create(objectUnderTest.deletePerson(new DeletePersonCommand(id)))
                    .verifyComplete();

            verify(eventPublisher).publish(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }
    }
}
