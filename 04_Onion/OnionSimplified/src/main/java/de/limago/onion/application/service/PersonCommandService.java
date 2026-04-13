package de.limago.onion.application.service;

import de.limago.onion.application.command.person.CorrectPersonCommand;
import de.limago.onion.application.command.person.CreatePersonCommand;
import de.limago.onion.application.command.person.DeletePersonCommand;
import de.limago.onion.application.command.person.MovePersonCommand;
import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.in.person.CorrectPersonUseCase;
import de.limago.onion.application.port.in.person.CreatePersonUseCase;
import de.limago.onion.application.port.in.person.DeletePersonUseCase;
import de.limago.onion.application.port.in.person.MovePersonUseCase;
import de.limago.onion.application.port.outport.BankAccountRepository;
import de.limago.onion.application.port.outport.DomainEventPublisher;
import de.limago.onion.application.port.outport.PersonRepository;
import de.limago.onion.domain.model.aggregate.PersonAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
@Transactional
public class PersonCommandService implements
        CreatePersonUseCase,
        MovePersonUseCase,
        CorrectPersonUseCase,
        DeletePersonUseCase {

    private final PersonRepository personRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<UUID> createPerson(CreatePersonCommand command) {
        PersonAggregate person = PersonAggregate.create(
                command.personId(),
                command.firstName(),
                command.lastName(),
                command.address()
        );
        return personRepository.save(person)
                .then(eventPublisher.publish(person.getDomainEvents()))
                .then(Mono.fromRunnable(person::clearDomainEvents))
                .thenReturn(command.personId());
    }

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<Void> movePerson(MovePersonCommand command) {
        return loadPerson(command.personId())
                .flatMap(person -> {
                    person.moveTo(command.newAddress());
                    return saveAndPublish(person);
                });
    }

    @Override
    @PreAuthorize("hasAnyRole('SACHBEARBEITER', 'VORGESETZTER')")
    public Mono<Void> correct(CorrectPersonCommand command) {
        return loadPerson(command.personId())
                .flatMap(person -> {
                    if (command.firstName().isPresent() || command.lastName().isPresent()) {
                        person.correctPerson(command.firstName(), command.lastName());
                    }
                    if (command.address().isPresent()) {
                        person.correctAddress(command.address().get());
                    }
                    return saveAndPublish(person);
                });
    }

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<Void> deletePerson(DeletePersonCommand command) {
        return loadPerson(command.personId())
                .flatMap(person -> {
                    person.markAsDeleted();
                    return personRepository.save(person)
                            .then(bankAccountRepository.deleteByPersonId(person.getId()))
                            .then(eventPublisher.publish(person.getDomainEvents()))
                            .then(Mono.fromRunnable(person::clearDomainEvents));
                });
    }

    private Mono<PersonAggregate> loadPerson(UUID id) {
        return personRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Person", id)));
    }

    private Mono<Void> saveAndPublish(PersonAggregate person) {
        return personRepository.save(person)
                .then(eventPublisher.publish(person.getDomainEvents()))
                .then(Mono.fromRunnable(person::clearDomainEvents));
    }
}
