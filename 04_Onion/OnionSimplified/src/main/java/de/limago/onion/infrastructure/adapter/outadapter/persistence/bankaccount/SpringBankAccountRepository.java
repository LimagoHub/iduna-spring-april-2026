package de.limago.onion.infrastructure.adapter.outadapter.persistence.bankaccount;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SpringBankAccountRepository extends ReactiveMongoRepository<BankAccountDocument, String> {
}
