package de.reactorbasics;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.image.ImageProducer;
import java.util.List;
import java.util.concurrent.Flow;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Reactor Basics ===\n");

        beispiel0_FluxAlsProducer();
        beispiel1_FluxAusListe();
        beispiel2_FluxMitOperatoren();
        beispiel3_Mono();
        beispiel4_PipelineWieSimpleFlow();
        beispiel5_Fehlerbehandlung();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 0: Flux ist ein Producer – lazy, nichts passiert ohne subscribe()
    // ---------------------------------------------------------------------------
    // Ein Flux beschreibt nur, WAS produziert werden soll – aber noch nicht WIE
    // und WANN. Erst subscribe() startet die eigentliche Produktion.
    //
    // Das ist der fundamentale Unterschied zu einer normalen Liste oder einem
    // Iterator: Dort existieren die Daten bereits im Speicher. Ein Flux ist eine
    // Beschreibung eines Datenflusses – eine "Rezept", das erst ausgeführt wird,
    // wenn ein Subscriber es anfordert.
    //
    // Dieses Beispiel macht das sichtbar: Die Produktion (das Lambda in
    // Flux.generate) wird erst aufgerufen, wenn subscribe() aufgerufen wird.
    // ---------------------------------------------------------------------------
    static void beispiel0_FluxAlsProducer() {
        System.out.println("--- Beispiel 0: Flux als Producer (lazy) ---");

        // Flux.generate() erzeugt Elemente on-demand, eines nach dem anderen.
        // Das Sink-Objekt ist der "Ausgang" des Producers – über sink.next()
        // wird ein Element in den Stream geschickt, sink.complete() beendet ihn.
        // Das Lambda wird erst aufgerufen, wenn ein Subscriber Elemente anfordert.
        Flux<Integer> producer = Flux.generate(
            () -> 1,                               // Startzustand: Zähler beginnt bei 1
            (zaehler, sink) -> {
                System.out.println("  [Producer] produziere Element " + zaehler);
                sink.next(zaehler);                // Element in den Stream senden
                if (zaehler == 3) sink.complete(); // nach 3 Elementen fertig
                return zaehler + 1;                // neuer Zustand für nächsten Aufruf
            }
        );



        // Bis hier: kein einziges "produziere Element" wurde ausgegeben.
        // Der Flux existiert nur als Beschreibung – noch kein Code läuft.
        System.out.println("  [Main] Flux ist definiert, aber noch kein subscribe()");
        System.out.println("  [Main] → Kein Element wurde produziert!");
        System.out.println("  [Main] Jetzt subscribe()...");

        // Erst hier startet die Produktion – und zwar element-weise auf Anfrage
        // des Subscribers (Backpressure: request(1) nach jedem onNext).
        producer.subscribe(
            n -> System.out.println("  [Subscriber] empfangen: " + n)
        );



        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: Flux aus einer Liste erzeugen
    // ---------------------------------------------------------------------------
    // Flux<T> ist der Reactor-Ersatz für Publisher<T> aus der Java Flow API.
    // Er kann 0 bis N Elemente emittieren, dann onComplete() oder onError().
    // ---------------------------------------------------------------------------
    static void beispiel1_FluxAusListe() {
        System.out.println("--- Beispiel 1: Flux aus Liste ---");

        List<String> worte = List.of("Hallo", "Reactor", "Welt");

        // Flux.fromIterable() erzeugt einen "cold" Publisher:
        // Die Daten fließen erst, wenn jemand subscribed.
        Flux<String> flux = Flux.fromIterable(worte);

        // subscribe() startet den Datenfluss.
        // Das Lambda ist der onNext-Handler (jedes Element).
        flux.subscribe(element -> System.out.println("  Empfangen: " + element));

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: Flux mit Operatoren transformieren
    // ---------------------------------------------------------------------------
    // Reactor bietet 100+ Operatoren – keine manuelle onNext-Implementierung nötig.
    // map() und filter() sind die häufigsten.
    // ---------------------------------------------------------------------------
    static void beispiel2_FluxMitOperatoren() {
        System.out.println("--- Beispiel 2: map() und filter() ---");

        Flux.range(1, 10)                          // emittiert 1, 2, 3, ..., 10
            .filter(n -> n % 2 == 0)               // nur gerade Zahlen: 2, 4, 6, 8, 10
            .map(n -> n * n)                       // quadrieren: 4, 16, 36, 64, 100
            .subscribe(
                n    -> System.out.println("  Wert: " + n),          // onNext
                err  -> System.err.println("  Fehler: " + err),      // onError
                ()   -> System.out.println("  Stream abgeschlossen") // onComplete
            );

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: Mono – Publisher für genau 0 oder 1 Element
    // ---------------------------------------------------------------------------
    // Mono<T> wird überall dort eingesetzt, wo ein einzelner Wert erwartet wird:
    // z.B. eine HTTP-Antwort, ein Datenbankdatensatz, ein berechnetes Ergebnis.
    // ---------------------------------------------------------------------------
    static void beispiel3_Mono() {
        System.out.println("--- Beispiel 3: Mono ---");

        // Mono mit einem Wert
        Mono<String> monoMitWert = Mono.just("Ich bin ein einziger Wert");
        monoMitWert.subscribe(wert -> System.out.println("  Mono-Wert: " + wert));

        // Mono ohne Wert (leerer Stream)
        Mono<String> leereMono = Mono.empty();
        leereMono.subscribe(
            wert -> System.out.println("  Das kommt nie"),
            err  -> System.err.println("  Fehler"),
            ()   -> System.out.println("  Leere Mono abgeschlossen (kein Wert)")
        );

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: Dieselbe Pipeline wie in 01_SimpleFlow
    // ---------------------------------------------------------------------------
    // In 01_SimpleFlow brauchten wir:
    //   - SubmissionPublisher<String>
    //   - MyProcessor (String → Integer via String::length)
    //   - EndSubscriber<Integer>
    //   - CountDownLatch für Synchronisation
    //
    // Mit Reactor: eine einzige Methodenkette, blockLast() für Synchronisation.
    // ---------------------------------------------------------------------------
    static void beispiel4_PipelineWieSimpleFlow() {
        System.out.println("--- Beispiel 4: Pipeline wie 01_SimpleFlow ---");

        List<String> worte = List.of("1", "2", "drei", "4", "fünf", "6", "7", "acht", "9", "zehn");

        Flux.fromIterable(worte)
            .map(String::length)       // Transformation: String → Integer (Wortlänge)
            .doOnNext(n -> System.out.println("  Empfangen: " + n))
            .doOnComplete(() -> System.out.println("  Stream vollständig verarbeitet."))
            .blockLast();              // Hauptthread wartet – Ersatz für CountDownLatch

        // Hinweis: blockLast() blockiert den aufrufenden Thread, bis der Stream
        // abgeschlossen ist. In produktivem reaktivem Code vermeidet man block*(),
        // hier dient es didaktisch als Vergleich zu CountDownLatch aus 01_SimpleFlow.

        System.out.println("  Alle Elemente verarbeitet. Programm beendet.\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: Fehlerbehandlung
    // ---------------------------------------------------------------------------
    // Reactor hat eingebaute Fehleroperatoren – kein try/catch in onNext nötig.
    // onErrorReturn() liefert einen Fallback-Wert bei Fehler.
    // ---------------------------------------------------------------------------
    static void beispiel5_Fehlerbehandlung() {
        System.out.println("--- Beispiel 5: Fehlerbehandlung ---");

        Flux.just("10", "zwanzig", "30")
            .map(Integer::parseInt)                  // "zwanzig" wirft NumberFormatException
            .onErrorReturn(-1)                       // bei Fehler: -1 als Fallback, Stream endet
            .subscribe(n -> System.out.println("  Wert: " + n));

        System.out.println();

        // onErrorContinue() – Fehler überspringen, Stream läuft weiter:
        Flux.just("10", "zwanzig", "30")
            .map(Integer::parseInt)
            .onErrorContinue((err, obj) ->
                System.out.println("  Übersprungen '" + obj + "': " + err.getMessage()))
            .subscribe(n -> System.out.println("  Wert: " + n));

        System.out.println();
    }
}
