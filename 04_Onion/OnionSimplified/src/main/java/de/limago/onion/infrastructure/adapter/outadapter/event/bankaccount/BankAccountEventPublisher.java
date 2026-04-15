package de.limago.onion.infrastructure.adapter.outadapter.event.bankaccount;


import de.limago.onion.domain.person.event.BankAccountChangedEvent;
import de.limago.onion.domain.person.event.BankAccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Driven Adapter: veröffentlicht BankAccount-Domain-Events auf dedizierten Kafka-Topics.
 *
 * Vollständig unabhängig von PersonEventPublisher — Person-Events berühren diese Klasse nicht.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BankAccountEventPublisher {

    private static final String BANKACCOUNT_CREATED = "bankaccount-created";
    private static final String BANKACCOUNT_CHANGED = "bankaccount-changed";

    private final StreamBridge streamBridge;

    @Async
    @EventListener
    public void on(BankAccountCreatedEvent e) {
        log.info("[EVENT] BankAccountCreatedEvent | eventId: {} | Person {} | IBAN: {} | Bank: {}",
                e.eventId(), e.personId(), e.bankDetails().iban(), e.bankDetails().bankName());
        streamBridge.send(BANKACCOUNT_CREATED, e);
    }

    @Async
    @EventListener
    public void on(BankAccountChangedEvent e) {
        log.info("[EVENT] BankAccountChangedEvent | eventId: {} | Person {} | IBAN: {} → {}",
                e.eventId(), e.personId(), e.oldDetails().iban(), e.newDetails().iban());
        streamBridge.send(BANKACCOUNT_CHANGED, e);
    }
}
