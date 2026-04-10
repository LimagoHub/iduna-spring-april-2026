package de.subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

/**
 * Ein einfacher Endverbraucher (Subscriber) im Reactive-Streams-Modell.
 *
 * <p>Im Reactive-Streams-Protokoll (definiert in {@link java.util.concurrent.Flow})
 * ist ein {@link Subscriber} der letzte Empfänger in einer Verarbeitungskette.
 * Er abonniert einen Publisher oder Processor und reagiert auf vier Ereignisse:</p>
 *
 * <ul>
 *   <li>{@link #onSubscribe(Subscription)} – Handshake: Publisher bestätigt das Abonnement</li>
 *   <li>{@link #onNext(Object)}            – Normaler Datenempfang</li>
 *   <li>{@link #onError(Throwable)}        – Fehlerfall: Stream bricht ab</li>
 *   <li>{@link #onComplete()}              – Normale Terminierung: alle Daten gesendet</li>
 * </ul>
 *
 * <p>Die Methode {@link #awaitCompletion()} ermöglicht es dem aufrufenden Thread
 * (z.B. dem Main-Thread), auf die vollständige Verarbeitung zu warten, ohne
 * auf fragile Timing-Hacks wie {@code Thread.sleep()} angewiesen zu sein.</p>
 *
 * @param <T> der Typ der empfangenen Datenelement
 */
public class EndSubscriber<T> implements Subscriber<T> {

    private Subscription subscription;

    /**
     * Synchronisierungsmittel: Der Latch wird auf 1 gesetzt und in {@link #onComplete()}
     * heruntergezählt. Wer auf {@link #awaitCompletion()} wartet, wird dadurch
     * freigegeben, sobald der Stream vollständig abgeschlossen ist.
     *
     * Alternativ zu einem Latch wäre ein {@link java.util.concurrent.CompletableFuture}
     * denkbar – der Latch ist hier didaktisch klarer.
     */
    private final CountDownLatch completionLatch = new CountDownLatch(1);

    /**
     * Wird vom Publisher aufgerufen, sobald das Abonnement bestätigt wurde.
     *
     * <p>Über die {@link Subscription} steuert der Subscriber den Datenfluss (Backpressure):
     * {@code subscription.request(n)} fordert genau {@code n} weitere Elemente an.
     * Hier wird sofort das erste Element angefordert.</p>
     *
     * <p><b>Backpressure:</b> Durch den Request-Mechanismus kann der Subscriber
     * verhindern, überflutet zu werden. Das ist ein Kernprinzip von Reactive Streams.</p>
     */
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1); // erstes Element anfordern
    }

    /**
     * Wird für jedes empfangene Datenelement aufgerufen.
     *
     * <p>Nach der Verarbeitung wird sofort das nächste Element angefordert
     * (request(1) = ein-Element-pro-Schritt, d.h. kein Batching).
     * Das ermöglicht eine einfache, kontrollierte Verarbeitung.</p>
     */
    @Override
    public void onNext(T item) {
        System.out.println("Empfangen: " + item);
        subscription.request(1); // nächstes Element anfordern
    }

    /**
     * Wird bei einer unnormalen Terminierung aufgerufen (Fehler im Publisher/Processor).
     *
     * <p>Nach {@code onError} darf kein weiteres {@code onNext} mehr folgen.
     * Der Latch wird auch im Fehlerfall freigegeben, damit der Main-Thread
     * nicht hängen bleibt.</p>
     */
    @Override
    public void onError(Throwable throwable) {
        System.err.println("Fehler im Stream: " + throwable.getMessage());
        throwable.printStackTrace();
        completionLatch.countDown(); // auch im Fehlerfall freigeben
    }

    /**
     * Wird bei normaler Terminierung aufgerufen – der Publisher hat alle Elemente gesendet.
     *
     * <p>Hier wird der {@link CountDownLatch} heruntergezählt, um wartende Threads
     * (typischerweise den Main-Thread) zu benachrichtigen, dass die Verarbeitung
     * vollständig abgeschlossen ist.</p>
     */
    @Override
    public void onComplete() {
        System.out.println("Stream vollständig verarbeitet.");
        completionLatch.countDown(); // Main-Thread freigeben
    }

    /**
     * Blockiert den aufrufenden Thread, bis {@link #onComplete()} oder
     * {@link #onError(Throwable)} aufgerufen wurde.
     *
     * <p>Dies ist die korrekte Alternative zu einem {@code Thread.sleep()} oder
     * dem problematischen {@code ExecutorService.awaitTermination()} auf dem
     * gemeinsamen {@code ForkJoinPool.commonPool()}.</p>
     *
     * @throws InterruptedException wenn der wartende Thread unterbrochen wird
     */
    public void awaitCompletion() throws InterruptedException {
        completionLatch.await();
    }
}
