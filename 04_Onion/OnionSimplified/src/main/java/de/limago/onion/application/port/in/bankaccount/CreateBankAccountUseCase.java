package de.limago.onion.application.port.in.bankaccount;

import de.limago.onion.application.command.bankaccount.CreateBankAccountCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreateBankAccountUseCase {
    Mono<UUID> createBankAccount(CreateBankAccountCommand command);
}
