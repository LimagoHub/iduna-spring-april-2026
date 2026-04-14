package de.limago.onion.application.exception;

public class BankAccountServiceException extends RuntimeException {

    public BankAccountServiceException(Throwable cause) {
        super("Technischer Fehler im BankAccount-Service", cause);
    }
}
