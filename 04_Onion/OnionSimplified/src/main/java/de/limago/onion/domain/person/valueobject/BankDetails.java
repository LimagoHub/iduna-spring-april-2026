package de.limago.onion.domain.person.valueobject;

public record BankDetails(
        String iban,
        String bic,
        String bankName
) {
    public BankDetails {
        if (iban == null || iban.isBlank()) throw new IllegalArgumentException("IBAN darf nicht leer sein");
        if (bic == null || bic.isBlank()) throw new IllegalArgumentException("BIC darf nicht leer sein");
    }
}
