package de.limago.onion.application.queryside.service;

import de.limago.onion.application.port.inport.bankaccount.FindBankAccountUseCase;
import de.limago.onion.application.port.outport.BankAccountRepository;
import de.limago.onion.application.queryside.query.bankaccount.BankAccountResult;
import de.limago.onion.application.queryside.query.bankaccount.FindBankAccountByPersonIdQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankAccountQueryService implements FindBankAccountUseCase {

    private final BankAccountRepository bankAccountRepository;

    @Override
    public Mono<BankAccountResult> findByPersonId(FindBankAccountByPersonIdQuery query) {
        return bankAccountRepository.findByPersonId(query.personId())
                .map(a -> new BankAccountResult(a.getPersonId(), a.getBankDetails()));
    }
}
