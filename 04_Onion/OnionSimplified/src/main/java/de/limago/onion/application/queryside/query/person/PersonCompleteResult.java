package de.limago.onion.application.queryside.query.person;

import de.limago.onion.domain.person.valueobject.Address;
import de.limago.onion.domain.person.valueobject.BankDetails;

import java.util.Optional;
import java.util.UUID;

public record PersonCompleteResult(
        UUID id,
        String firstName,
        String lastName,
        Address address,
        Optional<BankDetails> bankDetails
) {}
