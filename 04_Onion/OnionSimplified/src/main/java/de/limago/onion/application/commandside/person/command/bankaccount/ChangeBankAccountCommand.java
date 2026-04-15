package de.limago.onion.application.commandside.person.command.bankaccount;



import de.limago.onion.domain.person.valueobject.BankDetails;

import java.util.UUID;

public record ChangeBankAccountCommand(
        UUID personId,
        BankDetails newBankDetails
) {}
