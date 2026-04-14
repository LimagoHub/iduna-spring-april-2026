package de.limago.onion.application.exception;

import java.util.UUID;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String entityName, UUID id) {
        super(entityName + " existiert bereits: " + id);
    }
}
