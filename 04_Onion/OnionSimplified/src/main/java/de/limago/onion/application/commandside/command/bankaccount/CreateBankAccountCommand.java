package de.limago.onion.application.commandside.command.bankaccount;

import de.limago.onion.domain.model.valueobject.BankDetails;

import java.util.UUID;

public record CreateBankAccountCommand(
        UUID personId,
        BankDetails bankDetails
) {}
