package de.limago.onion.application.shared;

import java.util.UUID;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String entityName, UUID id) {
        super(entityName + " existiert bereits: " + id);
    }
}
