package de.limago.onion.application.queryside.query.bankaccount;

import de.limago.onion.domain.person.valueobject.BankDetails;

import java.util.UUID;

public record BankAccountResult(
        UUID personId,
        BankDetails bankDetails
) {}
