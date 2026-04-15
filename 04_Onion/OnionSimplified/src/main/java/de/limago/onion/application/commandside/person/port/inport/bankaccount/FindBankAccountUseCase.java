package de.limago.onion.application.commandside.person.port.inport.bankaccount;

import de.limago.onion.application.queryside.query.bankaccount.BankAccountResult;
import de.limago.onion.application.queryside.query.bankaccount.FindBankAccountByPersonIdQuery;
import reactor.core.publisher.Mono;

public interface FindBankAccountUseCase {
    Mono<BankAccountResult> findByPersonId(FindBankAccountByPersonIdQuery query);
}
