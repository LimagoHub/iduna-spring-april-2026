package de.limago.onion.application.shared;

import reactor.core.publisher.Mono;

import java.util.List;

public interface DomainEventPublisher {
    Mono<Void> publish(List<Object> events);
}
