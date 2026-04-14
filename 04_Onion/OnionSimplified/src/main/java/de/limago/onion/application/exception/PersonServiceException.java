package de.limago.onion.application.exception;

public class PersonServiceException extends RuntimeException {

    public PersonServiceException(Throwable cause) {
        super("Technischer Fehler im Person-Service", cause);
    }
}
