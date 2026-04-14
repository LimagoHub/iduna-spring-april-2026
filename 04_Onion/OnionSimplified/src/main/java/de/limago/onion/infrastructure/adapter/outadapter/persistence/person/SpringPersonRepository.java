package de.limago.onion.infrastructure.adapter.outadapter.persistence.person;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SpringPersonRepository extends ReactiveMongoRepository<PersonDocument, String> {
}
