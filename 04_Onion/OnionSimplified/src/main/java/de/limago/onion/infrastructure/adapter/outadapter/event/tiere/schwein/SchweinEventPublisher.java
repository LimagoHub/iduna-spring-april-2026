package de.limago.onion.infrastructure.adapter.outadapter.event.tiere.schwein;

import de.limago.onion.domain.tiere.schwein.event.ScheinDeletedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinCreatedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinMaxWeightReachedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Driven Adapter: veröffentlicht Schwein-Domain-Events auf dedizierten Kafka-Topics.
 *
 * SchweinUpdatedEvent ist ein internes Ereignis (Umbenennung/Gewichtsänderung) —
 * nur geloggt, kein Kafka. SchweinMaxWeightReachedEvent ist fachlich relevant
 * für nachgelagerte Systeme → Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SchweinEventPublisher {

    private static final String SCHWEIN_CREATED     = "schwein-created";
    private static final String SCHWEIN_DELETED     = "schwein-deleted";
    private static final String SCHWEIN_MAX_WEIGHT  = "schwein-max-weight-reached";

    private final StreamBridge streamBridge;

    @Async
    @EventListener
    public void on(SchweinCreatedEvent e) {
        log.info("[EVENT] SchweinCreatedEvent | eventId: {} | Schwein '{}' angelegt | Gewicht: {} kg",
                e.eventId(), e.name(), e.gewicht());
        streamBridge.send(SCHWEIN_CREATED, e);
    }

    @Async
    @EventListener
    public void on(SchweinUpdatedEvent e) {
        log.info("[EVENT] SchweinUpdatedEvent | eventId: {} | Schwein '{}' | Gewicht: {} kg",
                e.eventId(), e.name(), e.gewicht());
    }

    @Async
    @EventListener
    public void on(SchweinMaxWeightReachedEvent e) {
        log.warn("[EVENT] SchweinMaxWeightReachedEvent | eventId: {} | Schwein '{}' hat Maximalgewicht {} kg erreicht!",
                e.eventId(), e.name(), e.gewicht());
        streamBridge.send(SCHWEIN_MAX_WEIGHT, e);
    }

    @Async
    @EventListener
    public void on(ScheinDeletedEvent e) {
        log.info("[EVENT] ScheinDeletedEvent | eventId: {} | Schwein '{}' gelöscht",
                e.eventId(), e.name());
        streamBridge.send(SCHWEIN_DELETED, e);
    }
}
