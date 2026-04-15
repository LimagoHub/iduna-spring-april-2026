package de.limago.onion.application.commandside.person.exception;

public class BankAccountServiceException extends RuntimeException {

    public BankAccountServiceException(Throwable cause) {
        super("Technischer Fehler im BankAccount-Service", cause);
    }
}
