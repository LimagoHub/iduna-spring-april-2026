package de.limago.onion.infrastructure.adapter.outadapter.event;


import de.limago.onion.domain.model.aggregate.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Driven Adapter: veröffentlicht Person-Domain-Events auf dedizierten Kafka-Topics.
 *
 * Jede Methode ist direkt auf einen konkreten Event-Typ typisiert —
 * kein Switch, kein Wrapper. Ein neues BankAccount-Event berührt diese Klasse nicht.
 *
 * PersonChangedEvent und PersonAddressCorrectedEvent sind interne Korrekturen
 * ohne systemübergreifende Relevanz → nur geloggt, kein Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PersonEventPublisher {

    private static final String PERSON_CREATED = "person-created";
    private static final String PERSON_DELETED = "person-deleted";
    private static final String PERSON_MOVED   = "person-moved";

    private final StreamBridge streamBridge;

    @Async
    @EventListener
    public void on(PersonCreatedEvent e) {
        log.info("[EVENT] PersonCreatedEvent | eventId: {} | Person {} {} angelegt | ID: {} | Adresse: {}, {}, {}",
                e.eventId(), e.firstName(), e.lastName(), e.personId(),
                e.address().street(), e.address().postalCode(), e.address().city());
        streamBridge.send(PERSON_CREATED, e);
    }

    @Async
    @EventListener
    public void on(PersonDeletedEvent e) {
        log.info("[EVENT] PersonDeletedEvent | eventId: {} | Person {} gelöscht am {}",
                e.eventId(), e.personId(), e.deletedAt());
        streamBridge.send(PERSON_DELETED, e);
    }

    @Async
    @EventListener
    public void on(PersonMovedEvent e) {
        log.info("[EVENT] PersonMovedEvent | eventId: {} | Person {} | {} {} → {} {}",
                e.eventId(), e.personId(),
                e.oldAddress().street(), e.oldAddress().city(),
                e.newAddress().street(), e.newAddress().city());
        streamBridge.send(PERSON_MOVED, e);
    }

    @Async
    @EventListener
    public void on(PersonChangedEvent e) {
        log.info("[EVENT] PersonChangedEvent | eventId: {} | Person {} | Vorname: {} | Nachname: {}",
                e.eventId(), e.personId(),
                e.newFirstName().orElse("-"),
                e.newLastName().orElse("-"));
    }

    @Async
    @EventListener
    public void on(PersonAddressCorrectedEvent e) {
        log.info("[EVENT] PersonAddressCorrectedEvent | eventId: {} | Person {} | Korrektur: {} {} → {} {}",
                e.eventId(), e.personId(),
                e.oldAddress().street(), e.oldAddress().city(),
                e.correctedAddress().street(), e.correctedAddress().city());
    }
}
