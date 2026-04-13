package de.limago.onion.application.port.in.bankaccount;

import de.limago.onion.application.command.bankaccount.ChangeBankAccountCommand;
import reactor.core.publisher.Mono;

public interface ChangeBankAccountUseCase {
    Mono<Void> changeBankAccount(ChangeBankAccountCommand command);
}
