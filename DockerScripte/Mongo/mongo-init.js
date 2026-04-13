// mongo-init.js
db = db.getSiblingDB('oniondb');

// Persons
db.persons.insertMany([
    {
        _id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        version: 0,
        firstName: "Max",
        lastName: "Mustermann",
        address: {
            street: "Hauptstraße",
            houseNumber: "42",
            postalCode: "64283",
            city: "Darmstadt",
            country: "DE"
        },
        deletedAt: null
    },
    {
        _id: "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        version: 0,
        firstName: "Erika",
        lastName: "Musterfrau",
        address: {
            street: "Rheinstraße",
            houseNumber: "7a",
            postalCode: "64283",
            city: "Darmstadt",
            country: "DE"
        },
        deletedAt: null
    },
    {
        _id: "c3d4e5f6-a7b8-9012-cdef-123456789012",
        version: 0,
        firstName: "Hans",
        lastName: "Gelöscht",
        address: {
            street: "Musterweg",
            houseNumber: "1",
            postalCode: "10115",
            city: "Berlin",
            country: "DE"
        },
        deletedAt: new Date("2024-06-01T10:00:00Z")
    }
]);

// BankAccounts – _id = personId (shared key, 1:1)
db.bank_accounts.insertMany([
    {
        _id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        version: 0,
        iban: "DE89370400440532013000",
        bic: "COBADEFFXXX",
        bankName: "Commerzbank"
    },
    {
        _id: "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        version: 0,
        iban: "DE75512108001245126199",
        bic: "BELADEBEXXX",
        bankName: "Berliner Sparkasse"
    }
]);