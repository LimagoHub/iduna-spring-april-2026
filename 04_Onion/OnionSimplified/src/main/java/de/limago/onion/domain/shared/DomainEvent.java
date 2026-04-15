package de.limago.onion.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Basisvertrag für alle Domain Events.
 *
 * eventId    – eindeutige ID dieses Ereignisses (nicht der Entität).
 *              Ermöglicht Idempotenz auf Consumer-Seite: dasselbe Event
 *              kann sicher mehrfach verarbeitet werden, wenn der Consumer
 *              bereits verarbeitete IDs verfolgt.
 *
 * occurredAt – fachlicher Zeitstempel: wann das Ereignis eingetreten ist.
 *              Bei Replay-Szenarien bleibt dieser Zeitstempel unverändert —
 *              er ist nicht "wann das Event zuletzt verarbeitet wurde".
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
}
