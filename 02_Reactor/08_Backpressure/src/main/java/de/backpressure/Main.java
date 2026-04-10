package de.backpressure;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Backpressure ===\n");

        beispiel1_BaseSubscriber();
        beispiel2_LimitRate();
        beispiel3_OnBackpressureBuffer();
        beispiel4_BufferMitLimit();
        beispiel5_OnBackpressureDrop();
        beispiel6_OnBackpressureLatest();
        beispiel7_Vergleich();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: BaseSubscriber – manuelles request()
    // ---------------------------------------------------------------------------
    // Das Reactive-Streams-Protokoll verlangt, dass der Subscriber dem Publisher
    // mitteilt, wie viele Elemente er verarbeiten kann: request(n).
    //
    // Mit BaseSubscriber kann man diesen Mechanismus direkt steuern:
    //   hookOnSubscribe()  → wird aufgerufen, wenn Subscription etabliert ist
    //   hookOnNext()       → wird aufgerufen, wenn ein Element ankommt
    //
    // request(1) nach jedem Element = klassisches "one-by-one pull"
    // request(Long.MAX_VALUE) = unbegrenzt (kein Backpressure, wie subscribe())
    //
    // Wann verwenden?
    //   - Wenn man den Fluss exakt steuern will (z.B. erst nach DB-Write das nächste)
    //   - Für eigene Operatoren oder Tests
    // ---------------------------------------------------------------------------
    static void beispiel1_BaseSubscriber() throws InterruptedException {
        System.out.println("--- Beispiel 1: BaseSubscriber – manuelles request() ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 10)
            .subscribe(new BaseSubscriber<Integer>() {
                @Override
                protected void hookOnSubscribe(Subscription subscription) {
                    System.out.println("  Subscribed – fordere erstes Element an");
                    request(1);  // nur 1 Element auf einmal anfordern
                }

                @Override
                protected void hookOnNext(Integer value) {
                    System.out.println("  Empfangen: " + value + " → verarbeite...");
                    request(1);  // nach Verarbeitung: nächstes Element anfordern
                }

                @Override
                protected void hookOnComplete() {
                    System.out.println("  Fertig!");
                    latch.countDown();
                }
            });

        latch.await();
        System.out.println("  → request(1) steuert den Fluss Element für Element\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: limitRate() – Prefetch-Batches steuern
    // ---------------------------------------------------------------------------
    // limitRate(n) begrenzt, wie viele Elemente der Publisher auf einmal
    // vorausproduieren darf (Prefetch). Reactor fordert intern immer n Elemente
    // an und füllt nach, wenn 75% verbraucht sind (= 0,75 * n).
    //
    // Typisch beim Arbeiten mit Datenbanken oder Netzwerkquellen, wo man
    // nicht den gesamten Datensatz auf einmal im Speicher haben will.
    //
    // limitRate(n, lowTide) erlaubt separate Kontrolle des Nachfüll-Schwellwerts.
    //
    // Wann verwenden?
    //   - Pagination von Datenbankabfragen
    //   - Kontrolle des Speicherverbrauchs bei großen Streams
    //   - Kombination mit publishOn() für Threading
    // ---------------------------------------------------------------------------
    static void beispiel2_LimitRate() throws InterruptedException {
        System.out.println("--- Beispiel 2: limitRate() – Prefetch steuern ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 20)
            .limitRate(5)        // maximal 5 Elemente auf einmal vorausproduzierten
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                n -> System.out.println("  Empfangen: " + n),
                err -> System.err.println("Fehler: " + err),
                () -> { System.out.println("  Fertig!"); latch.countDown(); }
            );

        latch.await();
        System.out.println("  → limitRate(5): Reactor fordert intern je 5 Elemente an\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: onBackpressureBuffer() – Puffer ohne Limit
    // ---------------------------------------------------------------------------
    // Wenn ein Publisher schneller produziert als der Subscriber konsumiert,
    // gibt es einen Backpressure-Konflikt. onBackpressureBuffer() puffert
    // alle Elemente, die der Subscriber noch nicht angefordert hat.
    //
    // Ohne Limit: wächst unbegrenzt (Speicherproblem bei langen Streams).
    // Mit dem Scheduler-Trick publishOn() entsteht der Konflikt: der Publisher
    // produziert synchron, der Subscriber konsumiert in einem anderen Thread.
    //
    // Wann verwenden?
    //   - Wenn kurze Bursts (Spitzen) gepuffert werden sollen
    //   - Wenn der Subscriber gelegentlich langsamer ist, aber aufholt
    // ---------------------------------------------------------------------------
    static void beispiel3_OnBackpressureBuffer() throws InterruptedException {
        System.out.println("--- Beispiel 3: onBackpressureBuffer() – Puffer ohne Limit ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 10)
            .onBackpressureBuffer()           // puffere alle nicht angeforderten Elemente
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                n -> {
                    System.out.println("  Verarbeite: " + n);
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                },
                err -> System.err.println("Fehler: " + err),
                () -> { System.out.println("  Fertig!"); latch.countDown(); }
            );

        latch.await();
        System.out.println("  → alle Elemente gepuffert, kein Element verloren\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: onBackpressureBuffer(n) – Puffer mit Limit und Overflow-Handler
    // ---------------------------------------------------------------------------
    // Mit einem Limit (n) wirft onBackpressureBuffer() einen Fehler (oder ruft
    // einen Handler auf), wenn der Puffer voll ist.
    //
    // Varianten des Overflow-Handlers:
    //   onBackpressureBuffer(n, element -> ...) → eigener Handler pro verworfenem Element
    //   onBackpressureBuffer(n, null, BufferOverflowStrategy.DROP_OLDEST) → Strategie
    //
    // Hier: einfacher Handler, der verworfene Elemente loggt.
    //
    // Wann verwenden?
    //   - Wenn Speicher begrenzt ist und man Overflow explizit behandeln will
    //   - Wenn ein voller Puffer ein Fehlerfall ist (fail-fast)
    // ---------------------------------------------------------------------------
    static void beispiel4_BufferMitLimit() throws InterruptedException {
        System.out.println("--- Beispiel 4: onBackpressureBuffer(n) – Puffer mit Limit ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 30)
            .onBackpressureBuffer(
                5,                                        // max. 5 Elemente im Puffer
                verworfen -> System.out.println("  !! Puffer voll – verworfen: " + verworfen)
            )
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                n -> {
                    System.out.println("  Verarbeite: " + n);
                    try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                },
                err -> { System.out.println("  Fehler (Puffer überlaufen): " + err.getMessage()); latch.countDown(); },
                () -> { System.out.println("  Fertig!"); latch.countDown(); }
            );

        latch.await();
        System.out.println("  → bei Pufferüberlauf: eigener Handler + Fehler-Signal\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: onBackpressureDrop() – Elemente verwerfen
    // ---------------------------------------------------------------------------
    // Anstatt zu puffern, verwirft onBackpressureDrop() alle Elemente, die der
    // Subscriber gerade nicht anfordern kann. Es geht kein Fehler, aber Elemente
    // gehen verloren.
    //
    // Optional: ein Handler pro verworfenem Element (z.B. zum Zählen oder Loggen).
    //
    // Wann verwenden?
    //   - Wenn neuere Daten wichtiger sind als ältere (z.B. Sensor-Readings)
    //   - Wenn Datenverlust akzeptabel ist (Metriken, Sampling)
    //   - Wenn kein Puffer Speicher fressen soll
    // ---------------------------------------------------------------------------
    static void beispiel5_OnBackpressureDrop() throws InterruptedException {
        System.out.println("--- Beispiel 5: onBackpressureDrop() – Elemente verwerfen ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 20)
            .onBackpressureDrop(verworfen -> System.out.println("  DROP: " + verworfen))
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                n -> {
                    System.out.println("  Empfangen: " + n);
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                },
                err -> System.err.println("Fehler: " + err),
                () -> { System.out.println("  Fertig!"); latch.countDown(); }
            );

        latch.await();
        System.out.println("  → nicht angeforderte Elemente werden stillschweigend verworfen\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: onBackpressureLatest() – nur neuestes Element halten
    // ---------------------------------------------------------------------------
    // onBackpressureLatest() ist wie onBackpressureDrop(), aber behält immer
    // das NEUESTE nicht ausgelieferte Element. Wenn der Subscriber wieder
    // bereit ist, bekommt er das zuletzt produzierte Element – nicht das erste.
    //
    // Unterschied zu onBackpressureDrop():
    //   Drop    → ältere und neuere Elemente werden verworfen, sobald der Slot belegt ist
    //   Latest  → der Slot wird immer mit dem neuesten Element überschrieben
    //
    // Wann verwenden?
    //   - Wenn nur der aktuelle Stand relevant ist (z.B. Cursor-Position, Preis)
    //   - Wenn "verpasste" Zwischenwerte irrelevant sind
    // ---------------------------------------------------------------------------
    static void beispiel6_OnBackpressureLatest() throws InterruptedException {
        System.out.println("--- Beispiel 6: onBackpressureLatest() – nur neuestes Element ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux.range(1, 20)
            .onBackpressureLatest()
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                n -> {
                    System.out.println("  Empfangen: " + n);
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                },
                err -> System.err.println("Fehler: " + err),
                () -> { System.out.println("  Fertig!"); latch.countDown(); }
            );

        latch.await();
        System.out.println("  → bei Überlastung: nur das neueste Element wird vorgehalten\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 7: Vergleich aller Strategien
    // ---------------------------------------------------------------------------
    // Übersicht: Wann welche Backpressure-Strategie?
    //
    //   request(n) / BaseSubscriber  → exakte Flusssteuerung, eigener Rhythmus
    //   limitRate(n)                 → Prefetch-Batches, kein Datenverlust
    //   onBackpressureBuffer()       → alle Elemente puffern (Speicher beachten!)
    //   onBackpressureBuffer(n)      → Puffer begrenzt + Overflow-Strategie
    //   onBackpressureDrop()         → Elemente verwerfen, neuere bevorzugt
    //   onBackpressureLatest()       → immer nur das neueste Element halten
    //
    // Entscheidungsbaum:
    //   Datenverlust akzeptabel?
    //     Nein → Buffer oder limitRate
    //     Ja → Drop oder Latest
    //         Letzte Version reicht? → Latest
    //         Alle verpassten ignorieren? → Drop
    // ---------------------------------------------------------------------------
    static void beispiel7_Vergleich() {
        System.out.println("--- Beispiel 7: Strategien im Überblick ---");

        System.out.println("""
                  Strategie              Datenverlust?  Speicher      Wann verwenden?
                  ─────────────────────────────────────────────────────────────────────
                  request(n)             Nein           kontrolliert  eigener Rhythmus
                  limitRate(n)           Nein           begrenzt      Pagination, DB
                  onBackpressureBuffer() Nein           unbegrenzt!   kurze Bursts
                  onBackpressureBuffer(n)Nein/Error     begrenzt      Fail-fast
                  onBackpressureDrop()   Ja             keiner        Metriken, Sampling
                  onBackpressureLatest() Ja (nur letzt) keiner        Cursor, Preise
                """);
    }
}
