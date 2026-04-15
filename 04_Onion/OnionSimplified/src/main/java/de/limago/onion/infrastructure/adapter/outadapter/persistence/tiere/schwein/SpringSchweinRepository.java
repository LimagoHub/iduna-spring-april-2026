package de.limago.onion.infrastructure.adapter.outadapter.persistence.tiere.schwein;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SpringSchweinRepository extends ReactiveMongoRepository<SchweinDocument, String> {
}
