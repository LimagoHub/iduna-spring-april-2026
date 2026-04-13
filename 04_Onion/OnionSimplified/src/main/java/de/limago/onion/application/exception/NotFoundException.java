package de.limago.onion.application.exception;

import java.util.UUID;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entityName, UUID id) {
        super(entityName + " nicht gefunden: " + id);
    }
}
