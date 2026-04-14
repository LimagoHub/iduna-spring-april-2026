package de.limago.onion.application.port.inport.bankaccount;

import de.limago.onion.application.commandside.command.bankaccount.ChangeBankAccountCommand;
import reactor.core.publisher.Mono;

public interface ChangeBankAccountUseCase {
    Mono<Void> changeBankAccount(ChangeBankAccountCommand command);
}
