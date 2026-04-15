package de.limago.onion.application.commandside.person.port.inport.bankaccount;

import de.limago.onion.application.commandside.person.command.bankaccount.CreateBankAccountCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreateBankAccountUseCase {
    Mono<UUID> createBankAccount(CreateBankAccountCommand command);
}
