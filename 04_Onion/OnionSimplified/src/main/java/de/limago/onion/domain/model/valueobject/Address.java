package de.limago.onion.domain.model.valueobject;

public record Address(
        String street,
        String houseNumber,
        String postalCode,
        String city,
        String country
) {
    public Address {
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Straße darf nicht leer sein");
        if (postalCode == null || postalCode.isBlank()) throw new IllegalArgumentException("PLZ darf nicht leer sein");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("Stadt darf nicht leer sein");
    }
}
