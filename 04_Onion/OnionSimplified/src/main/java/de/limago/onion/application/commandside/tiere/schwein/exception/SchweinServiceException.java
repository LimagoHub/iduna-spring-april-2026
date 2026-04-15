package de.limago.onion.application.commandside.tiere.schwein.exception;

public class SchweinServiceException extends RuntimeException {

    public SchweinServiceException(Throwable cause) {
        super("Technischer Fehler im Schwein-Service", cause);
    }
}
