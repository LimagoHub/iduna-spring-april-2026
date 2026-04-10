package de.sinks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

/**
 * Modul 10: Sinks
 *
 * Sinks sind programmatische Publisher – sie erlauben es, von aussen
 * (z.B. aus einem anderen Thread oder aus imperativem Code) Elemente
 * in eine reaktive Pipeline einzuspeisen.
 *
 * Wichtige Sink-Typen:
 *   Sinks.One<T>       – genau ein Element oder Fehler (wie ein einmaliges Versprechen)
 *   Sinks.Many<T>      – viele Elemente, verschiedene Strategien:
 *     unicast()        – genau ein Subscriber
 *     multicast()      – mehrere Subscriber, kein Replay
 *     replay()         – mehrere Subscriber, mit Replay-Puffer
 *
 * Thread-Sicherheit: tryEmit*() vs. emitNext() / emitValue()
 *   tryEmit*()  – nicht-blockierend, gibt EmitResult zurück
 *   emitNext()  – wirft Exception bei Fehler (mit konfigurierbarem EmitFailureHandler)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Modul 10: Sinks ===\n");

        beispiel1_SinksOne();
        beispiel2_SinksOneAsMono();
        beispiel3_SinksManyUnicast();
        beispiel4_SinksManyMulticast();
        beispiel5_SinksManyReplay();
        beispiel6_ThreadsicheresEmittieren();
        beispiel7_EmitResult();
    }

    // -------------------------------------------------------------------------
    // Beispiel 1: Sinks.One – ein einzelner Wert
    // -------------------------------------------------------------------------
    static void beispiel1_SinksOne() {
        System.out.println("--- Beispiel 1: Sinks.One ---");

        // Sinks.one() verhält sich wie ein Mono-Versprechen.
        // tryEmitValue() liefert genau ein Element an alle Subscriber.
        Sinks.One<String> sink = Sinks.one();

        Mono<String> mono = sink.asMono();

        // Subscriber registrieren (noch kein Element vorhanden)
        mono.subscribe(
            wert  -> System.out.println("  Empfangen: " + wert),
            fehler -> System.out.println("  Fehler: " + fehler)
        );

        // Erst jetzt den Wert einspeisen
        Sinks.EmitResult result = sink.tryEmitValue("Hallo Sink!");
        System.out.println("  EmitResult: " + result);  // OK

        // Zweiter Versuch schlägt fehl (One erlaubt nur einen Wert)
        Sinks.EmitResult result2 = sink.tryEmitValue("Noch ein Wert");
        System.out.println("  Zweiter EmitResult: " + result2);  // FAIL_ALREADY_TERMINATED
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 2: Sinks.One als Mono – Late-Subscriber
    // -------------------------------------------------------------------------
    static void beispiel2_SinksOneAsMono() {
        System.out.println("--- Beispiel 2: Sinks.One – Late-Subscriber ---");

        Sinks.One<Integer> sink = Sinks.one();

        // Wert ZUERST einspeisen
        sink.tryEmitValue(42);

        // Subscriber DANACH registrieren → bekommt den Wert trotzdem (gecacht)
        sink.asMono().subscribe(
            wert -> System.out.println("  Late-Subscriber empfängt: " + wert)
        );

        // Fehler statt Wert
        Sinks.One<String> errorSink = Sinks.one();
        errorSink.tryEmitError(new RuntimeException("Etwas ist schiefgelaufen"));
        errorSink.asMono().subscribe(
            wert  -> System.out.println("  Wert: " + wert),
            fehler -> System.out.println("  Fehler empfangen: " + fehler.getMessage())
        );
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 3: Sinks.Many – Unicast (genau ein Subscriber)
    // -------------------------------------------------------------------------
    static void beispiel3_SinksManyUnicast() {
        System.out.println("--- Beispiel 3: Sinks.Many – Unicast ---");

        // unicast() erlaubt genau EINEN Subscriber.
        // onBackpressureBuffer() puffert Elemente, bis der Subscriber bereit ist.
        Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<Integer> flux = sink.asFlux();

        flux.subscribe(
            wert -> System.out.println("  Subscriber 1 empfängt: " + wert)
        );

        sink.tryEmitNext(1);
        sink.tryEmitNext(2);
        sink.tryEmitNext(3);
        sink.tryEmitComplete();

        // Ein zweiter Subscriber würde fehlschlagen:
        flux.subscribe(
            wert -> System.out.println("  Subscriber 2: " + wert),
            fehler -> System.out.println("  Subscriber 2 Fehler: " + fehler.getMessage())
        );
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 4: Sinks.Many – Multicast (mehrere Subscriber, kein Replay)
    // -------------------------------------------------------------------------
    static void beispiel4_SinksManyMulticast() {
        System.out.println("--- Beispiel 4: Sinks.Many – Multicast ---");

        // multicast() erlaubt MEHRERE Subscriber gleichzeitig.
        // directBestEffort(): Elemente werden an alle aktuellen Subscriber geschickt.
        // Subscriber, die sich NACH einer Emission anmelden, verpassen frühere Werte.
        Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();

        Flux<String> flux = sink.asFlux();

        // Subscriber A meldet sich vor den Emissionen an
        flux.subscribe(wert -> System.out.println("  [A] empfängt: " + wert));

        sink.tryEmitNext("Nachricht 1");
        sink.tryEmitNext("Nachricht 2");

        // Subscriber B meldet sich NACH den ersten Emissionen an → verpasst sie
        flux.subscribe(wert -> System.out.println("  [B] empfängt: " + wert));

        sink.tryEmitNext("Nachricht 3");  // beide empfangen diese
        sink.tryEmitComplete();
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 5: Sinks.Many – Replay (Puffer für Late-Subscriber)
    // -------------------------------------------------------------------------
    static void beispiel5_SinksManyReplay() {
        System.out.println("--- Beispiel 5: Sinks.Many – Replay ---");

        // replay().all() – alle bisherigen Elemente werden für neue Subscriber gepuffert
        Sinks.Many<String> sink = Sinks.many().replay().all();

        sink.tryEmitNext("Alpha");
        sink.tryEmitNext("Beta");
        sink.tryEmitNext("Gamma");

        // Subscriber A – kommt erst jetzt, bekommt aber alle früheren Elemente
        System.out.println("  Subscriber A (replay all):");
        sink.asFlux().take(3).subscribe(
            wert -> System.out.println("    [A] " + wert)
        );

        // replay().limit(2) – nur die letzten 2 Elemente werden gepuffert
        Sinks.Many<Integer> limitedSink = Sinks.many().replay().limit(2);
        limitedSink.tryEmitNext(10);
        limitedSink.tryEmitNext(20);
        limitedSink.tryEmitNext(30);

        System.out.println("  Subscriber B (replay last 2):");
        limitedSink.asFlux().take(2).subscribe(
            wert -> System.out.println("    [B] " + wert)
        );
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 6: Thread-sicheres Emittieren aus mehreren Threads
    // -------------------------------------------------------------------------
    static void beispiel6_ThreadsicheresEmittieren() throws InterruptedException {
        System.out.println("--- Beispiel 6: Thread-sicheres Emittieren ---");

        // emitNext() mit Sinks.EmitFailureHandler.FAIL_FAST ist die einfachste
        // Variante für serialisierten Zugriff aus einem Thread.
        // Für echte Nebenläufigkeit: tryEmitNext() in einer Retry-Schleife
        // oder serialisierten Zugang sicherstellen.
        Sinks.Many<Integer> sink = Sinks.many().multicast().directBestEffort();

        CountDownLatch latch = new CountDownLatch(1);
        var ergebnis = new java.util.ArrayList<Integer>();

        sink.asFlux()
            .take(6)
            .doOnComplete(latch::countDown)
            .subscribe(ergebnis::add);

        // Zwei Threads emittieren abwechselnd – serialisiert durch tryEmit-Retry
        var executor = Executors.newFixedThreadPool(2);
        for (int t = 0; t < 2; t++) {
            final int thread = t;
            executor.submit(() -> {
                for (int i = 1; i <= 3; i++) {
                    // Retry-Schleife für Thread-Konkurrenz
                    Sinks.EmitResult result;
                    do {
                        result = sink.tryEmitNext(thread * 10 + i);
                    } while (result == Sinks.EmitResult.FAIL_NON_SERIALIZED);
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("  Empfangene Werte (Reihenfolge variiert): " + ergebnis);
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 7: EmitResult auswerten
    // -------------------------------------------------------------------------
    static void beispiel7_EmitResult() {
        System.out.println("--- Beispiel 7: EmitResult auswerten ---");

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // Subscriber noch nicht registriert – Elemente werden gepuffert
        sink.tryEmitNext("A");
        sink.tryEmitNext("B");

        // Complete senden
        Sinks.EmitResult r1 = sink.tryEmitComplete();
        System.out.println("  tryEmitComplete(): " + r1);  // OK

        // Nach Complete ist kein Emit mehr möglich
        Sinks.EmitResult r2 = sink.tryEmitNext("C");
        System.out.println("  tryEmitNext nach Complete: " + r2);  // FAIL_ALREADY_TERMINATED

        // Alle EmitResult-Werte:
        System.out.println();
        System.out.println("  Mögliche EmitResult-Werte:");
        System.out.println("    OK                    – Emission erfolgreich");
        System.out.println("    FAIL_TERMINATED        – Sink ist bereits abgeschlossen");
        System.out.println("    FAIL_ALREADY_TERMINATED– Complete/Error bereits gesendet");
        System.out.println("    FAIL_OVERFLOW          – Puffer voll (Backpressure)");
        System.out.println("    FAIL_ZERO_SUBSCRIBER   – Kein Subscriber vorhanden");
        System.out.println("    FAIL_NON_SERIALIZED    – Nebenläufiger Zugriff erkannt");
        System.out.println("    FAIL_CANCELLED         – Subscriber hat gecancelt");

        // Subscriber abholen (gepufferte Elemente "A" und "B")
        sink.asFlux().subscribe(
            wert -> System.out.println("  Gepufferter Wert: " + wert)
        );
        System.out.println();
    }
}
