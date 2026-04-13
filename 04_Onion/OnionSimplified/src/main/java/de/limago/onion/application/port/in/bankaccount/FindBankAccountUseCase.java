package de.limago.onion.application.port.in.bankaccount;

import de.limago.onion.application.query.bankaccount.BankAccountResult;
import de.limago.onion.application.query.bankaccount.FindBankAccountByPersonIdQuery;
import reactor.core.publisher.Mono;

public interface FindBankAccountUseCase {
    Mono<BankAccountResult> findByPersonId(FindBankAccountByPersonIdQuery query);
}
