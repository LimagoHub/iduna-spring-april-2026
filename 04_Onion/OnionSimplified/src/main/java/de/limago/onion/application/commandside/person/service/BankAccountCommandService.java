package de.limago.onion.application.commandside.person.service;

import de.limago.onion.application.commandside.person.command.bankaccount.ChangeBankAccountCommand;
import de.limago.onion.application.commandside.person.command.bankaccount.CreateBankAccountCommand;
import de.limago.onion.application.shared.AlreadyExistsException;
import de.limago.onion.application.commandside.person.exception.BankAccountServiceException;
import de.limago.onion.application.shared.NotFoundException;
import de.limago.onion.application.commandside.person.port.inport.bankaccount.ChangeBankAccountUseCase;
import de.limago.onion.application.commandside.person.port.inport.bankaccount.CreateBankAccountUseCase;
import de.limago.onion.application.commandside.person.port.outport.BankAccountRepository;
import de.limago.onion.application.shared.DomainEventPublisher;
import de.limago.onion.domain.person.aggregate.BankAccountAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Transactional
public class BankAccountCommandService implements
        CreateBankAccountUseCase,
        ChangeBankAccountUseCase {

    private final BankAccountRepository bankAccountRepository;
    private final DomainEventPublisher eventPublisher;

    private static final Predicate<Throwable> IS_TECHNICAL =
            e -> !(e instanceof NotFoundException) && !(e instanceof AlreadyExistsException);

    @Override
    @PreAuthorize("hasRole('VORGESETZTER')")
    public Mono<UUID> createBankAccount(CreateBankAccountCommand command) {
        return bankAccountRepository.findByPersonId(command.personId())
                .flatMap(existing -> Mono.<UUID>error(new AlreadyExistsException("Bankverbindung", command.personId())))
                .switchIfEmpty(Mono.defer(() -> {
                    BankAccountAggregate account = BankAccountAggregate.create(
                            command.personId(),
                            command.bankDetails()
                    );
                    return bankAccountRepository.save(account)
                            .then(eventPublisher.publish(account.getDomainEvents()))
                            .then(Mono.<Void>fromRunnable(account::clearDomainEvents))
                            .thenReturn(account.getPersonId());
                }))
                .onErrorMap(IS_TECHNICAL, BankAccountServiceException::new);
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
                            .then(Mono.<Void>fromRunnable(account::clearDomainEvents));
                })
                .onErrorMap(IS_TECHNICAL, BankAccountServiceException::new);
    }
}
