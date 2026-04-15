package de.limago.onion.application.commandside.person.port.inport.bankaccount;

import de.limago.onion.application.commandside.person.command.bankaccount.ChangeBankAccountCommand;
import reactor.core.publisher.Mono;

public interface ChangeBankAccountUseCase {
    Mono<Void> changeBankAccount(ChangeBankAccountCommand command);
}
