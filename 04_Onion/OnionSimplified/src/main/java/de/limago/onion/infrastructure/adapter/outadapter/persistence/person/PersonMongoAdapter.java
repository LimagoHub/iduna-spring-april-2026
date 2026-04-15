package de.limago.onion.infrastructure.adapter.outadapter.persistence.person;

import de.limago.onion.application.commandside.person.port.outport.PersonRepository;
import de.limago.onion.domain.person.aggregate.PersonAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PersonMongoAdapter implements PersonRepository {

    private final SpringPersonRepository springRepo;
    private final PersonDocumentMapper mapper;

    @Override
    public Mono<Void> save(PersonAggregate person) {
        return springRepo.save(mapper.toDocument(person)).then();
    }

    @Override
    public Mono<PersonAggregate> findById(UUID id) {
        return springRepo.findById(id.toString())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<PersonAggregate> findAll() {
        return springRepo.findAll()
                .map(mapper::toDomain);
    }
}
