package de.limago.onion.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Aktiviert Springs asynchrone Methodenausführung (@Async).
 * Wird genutzt, damit Domain-Event-Listener den reaktiven Haupt-Thread
 * nicht blockieren und auf einem separaten Thread-Pool ausgeführt werden.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
