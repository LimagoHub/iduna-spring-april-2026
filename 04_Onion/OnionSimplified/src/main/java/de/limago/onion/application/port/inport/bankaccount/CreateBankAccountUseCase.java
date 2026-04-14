package de.limago.onion.application.port.inport.bankaccount;

import de.limago.onion.application.commandside.command.bankaccount.CreateBankAccountCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreateBankAccountUseCase {
    Mono<UUID> createBankAccount(CreateBankAccountCommand command);
}
