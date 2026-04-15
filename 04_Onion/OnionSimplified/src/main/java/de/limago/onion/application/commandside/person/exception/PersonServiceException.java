package de.limago.onion.application.commandside.person.exception;

public class PersonServiceException extends RuntimeException {

    public PersonServiceException(Throwable cause) {
        super("Technischer Fehler im Person-Service", cause);
    }
}
