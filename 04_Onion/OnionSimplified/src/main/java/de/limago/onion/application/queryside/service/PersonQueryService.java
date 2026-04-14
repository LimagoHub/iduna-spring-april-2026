package de.limago.onion.application.queryside.service;

import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.inport.person.FindPersonCompleteUseCase;
import de.limago.onion.application.port.inport.person.FindPersonUseCase;
import de.limago.onion.application.port.outport.BankAccountRepository;
import de.limago.onion.application.port.outport.PersonRepository;
import de.limago.onion.application.queryside.query.person.FindPersonByIdQuery;
import de.limago.onion.application.queryside.query.person.FindPersonCompleteByIdQuery;
import de.limago.onion.application.queryside.query.person.PersonCompleteResult;
import de.limago.onion.application.queryside.query.person.PersonResult;
import de.limago.onion.domain.model.aggregate.PersonAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonQueryService implements
        FindPersonUseCase,
        FindPersonCompleteUseCase {

    private final PersonRepository personRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    public Mono<PersonResult> findById(FindPersonByIdQuery query) {
        return loadPerson(query.personId())
                .map(person -> new PersonResult(
                        person.getId(),
                        person.getFirstName(),
                        person.getLastName(),
                        person.getAddress()
                ));
    }

    @Override
    public Flux<PersonResult> findAll() {
        return personRepository.findAll()
                .map(person -> new PersonResult(
                        person.getId(),
                        person.getFirstName(),
                        person.getLastName(),
                        person.getAddress()
                ));
    }

    @Override
    public Mono<PersonCompleteResult> findCompleteById(FindPersonCompleteByIdQuery query) {
        return loadPerson(query.personId())
                .flatMap(person -> bankAccountRepository.findByPersonId(person.getId())
                        .map(account -> Optional.of(account.getBankDetails()))
                        .defaultIfEmpty(Optional.empty())
                        .map(bankDetails -> new PersonCompleteResult(
                                person.getId(),
                                person.getFirstName(),
                                person.getLastName(),
                                person.getAddress(),
                                bankDetails
                        ))
                );
    }

    @Override
    public Flux<PersonCompleteResult> findAllComplete() {
        return personRepository.findAll()
                .flatMap(person -> bankAccountRepository.findByPersonId(person.getId())
                        .map(account -> Optional.of(account.getBankDetails()))
                        .defaultIfEmpty(Optional.empty())
                        .map(bankDetails -> new PersonCompleteResult(
                                person.getId(),
                                person.getFirstName(),
                                person.getLastName(),
                                person.getAddress(),
                                bankDetails
                        ))
                );
    }

    private Mono<PersonAggregate> loadPerson(UUID id) {
        return personRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Person", id)));
    }
}
