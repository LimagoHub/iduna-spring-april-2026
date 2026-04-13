package de.limago.onion.application.service;

import de.limago.onion.application.command.bankaccount.ChangeBankAccountCommand;
import de.limago.onion.application.command.bankaccount.CreateBankAccountCommand;
import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.in.bankaccount.ChangeBankAccountUseCase;
import de.limago.onion.application.port.in.bankaccount.CreateBankAccountUseCase;
import de.limago.onion.application.port.out.BankAccountRepository;
import de.limago.onion.application.port.out.DomainEventPublisher;
import de.limago.onion.domain.model.aggregate.BankAccountAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
@Transactional
public class BankAccountCommandService implements
        CreateBankAccountUseCase,
        ChangeBankAccountUseCase {

    private final BankAccountRepository bankAccountRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<UUID> createBankAccount(CreateBankAccountCommand command) {
        BankAccountAggregate account = BankAccountAggregate.create(
                command.personId(),
                command.bankDetails()
        );
        return bankAccountRepository.save(account)
                .then(eventPublisher.publish(account.getDomainEvents()))
                .then(Mono.fromRunnable(account::clearDomainEvents))
                .thenReturn(account.getPersonId());
    }

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<Void> changeBankAccount(ChangeBankAccountCommand command) {
        return bankAccountRepository.findByPersonId(command.personId())
                .switchIfEmpty(Mono.error(new NotFoundException("Bankverbindung", command.personId())))
                .flatMap(account -> {
                    account.changeBankDetails(command.newBankDetails());
                    return bankAccountRepository.save(account)
                            .then(eventPublisher.publish(account.getDomainEvents()))
                            .then(Mono.fromRunnable(account::clearDomainEvents));
                });
    }
}
