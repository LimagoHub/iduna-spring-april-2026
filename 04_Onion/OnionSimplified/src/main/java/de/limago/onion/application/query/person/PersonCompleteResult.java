package de.limago.onion.application.query.person;

import de.limago.onion.domain.model.valueobject.Address;
import de.limago.onion.domain.model.valueobject.BankDetails;

import java.util.Optional;
import java.util.UUID;

public record PersonCompleteResult(
        UUID id,
        String firstName,
        String lastName,
        Address address,
        Optional<BankDetails> bankDetails
) {}
