package de.limago.onion.application.commandside.person.service;

import de.limago.onion.application.commandside.person.command.person.CorrectPersonCommand;
import de.limago.onion.application.commandside.person.command.person.CreatePersonCommand;
import de.limago.onion.application.commandside.person.command.person.DeletePersonCommand;
import de.limago.onion.application.commandside.person.command.person.MovePersonCommand;
import de.limago.onion.application.shared.AlreadyExistsException;
import de.limago.onion.application.shared.NotFoundException;
import de.limago.onion.application.commandside.person.exception.PersonServiceException;
import de.limago.onion.application.commandside.person.port.inport.person.CorrectPersonUseCase;
import de.limago.onion.application.commandside.person.port.inport.person.CreatePersonUseCase;
import de.limago.onion.application.commandside.person.port.inport.person.DeletePersonUseCase;
import de.limago.onion.application.commandside.person.port.inport.person.MovePersonUseCase;
import de.limago.onion.application.commandside.person.port.outport.BankAccountRepository;
import de.limago.onion.application.shared.DomainEventPublisher;
import de.limago.onion.application.commandside.person.port.outport.PersonRepository;
import de.limago.onion.domain.person.aggregate.PersonAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Predicate;

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

    private static final Predicate<Throwable> IS_TECHNICAL =
            e -> !(e instanceof NotFoundException) && !(e instanceof AlreadyExistsException);

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<UUID> createPerson(CreatePersonCommand command) {
        return personRepository.findById(command.personId())
                .flatMap(existing -> Mono.<UUID>error(new AlreadyExistsException("Person", command.personId())))
                .switchIfEmpty(Mono.defer(() -> {
                    PersonAggregate person = PersonAggregate.create(
                            command.personId(),
                            command.firstName(),
                            command.lastName(),
                            command.address()
                    );
                    return personRepository.save(person)
                            .then(eventPublisher.publish(person.getDomainEvents()))
                            .then(Mono.<Void>fromRunnable(person::clearDomainEvents))
                            .thenReturn(command.personId());
                }))
                .onErrorMap(IS_TECHNICAL, PersonServiceException::new);
    }

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<Void> movePerson(MovePersonCommand command) {
        return loadPerson(command.personId())
                .flatMap(person -> {
                    person.moveTo(command.newAddress());
                    return saveAndPublish(person);
                })
                .onErrorMap(IS_TECHNICAL, PersonServiceException::new);
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
                })
                .onErrorMap(IS_TECHNICAL, PersonServiceException::new);
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
                            .then(Mono.<Void>fromRunnable(person::clearDomainEvents));
                })
                .onErrorMap(IS_TECHNICAL, PersonServiceException::new);
    }

    private Mono<PersonAggregate> loadPerson(UUID id) {
        return personRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Person", id)));
    }

    private Mono<Void> saveAndPublish(PersonAggregate person) {
        return personRepository.save(person)
                .then(eventPublisher.publish(person.getDomainEvents()))
                .then(Mono.<Void>fromRunnable(person::clearDomainEvents));
    }
}
