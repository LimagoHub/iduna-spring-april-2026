package de.limago.onion.application.query.bankaccount;

import de.limago.onion.domain.model.valueobject.BankDetails;

import java.util.UUID;

public record BankAccountResult(
        UUID personId,
        BankDetails bankDetails
) {}
