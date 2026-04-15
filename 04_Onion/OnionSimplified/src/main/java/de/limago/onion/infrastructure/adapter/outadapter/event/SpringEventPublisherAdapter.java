package de.limago.onion.infrastructure.adapter.outadapter.event;

import de.limago.onion.application.shared.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Driven Adapter: implementiert DomainEventPublisher.
 *
 * Leitet Domain-Events direkt an Springs ApplicationEventPublisher weiter.
 * Die fachliche Verarbeitung (Logging, Kafka) liegt in den aggregat-spezifischen
 * Publishern: PersonEventPublisher, BankAccountEventPublisher.
 */
@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher springEventPublisher;

    @Override
    public Mono<Void> publish(List<Object> events) {
        return Mono.fromRunnable(() ->
                events.forEach(springEventPublisher::publishEvent)
        );
    }
}
