package de.debugging;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Signal;

import java.util.concurrent.CountDownLatch;

/**
 * Modul 12: Debugging
 *
 * Reaktive Pipelines sind schwer zu debuggen, weil der Stack-Trace
 * zum Fehler-Zeitpunkt nicht zeigt, wo die Pipeline AUFGEBAUT wurde.
 *
 * Werkzeuge:
 *   .log()                   – gibt alle Reactive-Streams-Signale aus
 *   .log("kategorie")        – mit Kategoriepräfix
 *   .log("kategorie", Level) – gefilterter Log-Level
 *   .checkpoint()            – Assembly-Trace bei Fehler (leichter als Hooks)
 *   .checkpoint("beschr.")   – mit erklärendem Text
 *   .checkpoint("beschr.", true) – mit vollständigem Stack-Trace
 *   Hooks.onOperatorDebug()  – globales Assembly-Tracing (teuer!)
 *   ReactorDebugAgent        – Java-Agent für Assembly-Traces ohne Laufzeitkosten
 *   .doOnNext()              – Side-Effect ohne Signaltransformation
 *   .doOnError()             – Side-Effect bei Fehler
 *   .doOnComplete()          – Side-Effect bei Abschluss
 *   .tap()                   – moderne Alternative zu doOn*-Kette
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Modul 12: Debugging ===\n");

        beispiel1_LogOperator();
        beispiel2_LogMitKategorie();
        beispiel3_DoOnSignals();
        beispiel4_Checkpoint();
        beispiel5_CheckpointMitBeschreibung();
        beispiel6_HooksOnOperatorDebug();
        beispiel7_DoOnEach();
    }

    // -------------------------------------------------------------------------
    // Beispiel 1: .log() – alle Signale sichtbar machen
    // -------------------------------------------------------------------------
    static void beispiel1_LogOperator() {
        System.out.println("--- Beispiel 1: .log() ---");
        System.out.println("  (Log-Ausgaben erscheinen auf System.err / Logger)\n");

        // .log() protokolliert: onSubscribe, request, onNext, onComplete/onError, cancel
        Flux.just("A", "B", "C")
            .log()           // alle Signale
            .map(String::toLowerCase)
            .subscribe(
                wert -> System.out.println("  Subscriber empfängt: " + wert)
            );

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 2: .log() mit Kategorie und Level
    // -------------------------------------------------------------------------
    static void beispiel2_LogMitKategorie() {
        System.out.println("--- Beispiel 2: .log() mit Kategorie ---");

        Flux.range(1, 4)
            .log("QUELLE")          // Kategorie im Log-Präfix sichtbar
            .map(i -> i * 2)
            .log("NACH_MAP")        // zweiter log-Operator weiter unten in der Kette
            .filter(i -> i > 4)
            .subscribe(
                wert -> System.out.println("  Ergebnis: " + wert)
            );

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 3: doOnNext / doOnError / doOnComplete / doOnSubscribe
    // -------------------------------------------------------------------------
    static void beispiel3_DoOnSignals() {
        System.out.println("--- Beispiel 3: doOn-Operatoren ---");

        Flux.just(1, 2, 3, 0, 4)
            .doOnSubscribe(sub -> System.out.println("  [doOnSubscribe] Pipeline gestartet"))
            .doOnNext(n -> System.out.println("  [doOnNext] Vor Division: " + n))
            .map(n -> 100 / n)   // Division durch 0 bei n=0
            .doOnNext(n -> System.out.println("  [doOnNext] Nach Division: " + n))
            .doOnError(err -> System.out.println("  [doOnError] Fehler abgefangen: " + err.getMessage()))
            .doOnComplete(() -> System.out.println("  [doOnComplete] Abgeschlossen"))
            .onErrorContinue((err, val) ->
                System.out.println("  [onErrorContinue] Überspringe Wert: " + val)
            )
            .subscribe(
                wert -> System.out.println("  Subscriber: " + wert),
                fehler -> System.out.println("  Subscriber-Fehler: " + fehler)
            );

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 4: .checkpoint() – Fehlerort eingrenzen
    // -------------------------------------------------------------------------
    static void beispiel4_Checkpoint() {
        System.out.println("--- Beispiel 4: .checkpoint() ---");
        System.out.println("  (Fehler-Stack-Trace enthält Checkpoint-Marker)\n");

        // Ohne checkpoint: Stack-Trace zeigt nur reaktiven Framework-Code
        // Mit checkpoint: Stack-Trace zeigt den Aufbauort der Pipeline
        Flux<Integer> pipeline = Flux.just(5, 4, 3, 2, 1, 0)
            .checkpoint()           // Marker ohne Beschreibung
            .map(n -> 10 / n);      // Fehler bei n=0

        pipeline.subscribe(
            wert  -> System.out.println("  Wert: " + wert),
            fehler -> System.out.println("  Fehler: " + fehler.getMessage()
                        + " (Checkpoint im Stack-Trace sichtbar)")
        );

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 5: .checkpoint() mit Beschreibung
    // -------------------------------------------------------------------------
    static void beispiel5_CheckpointMitBeschreibung() {
        System.out.println("--- Beispiel 5: .checkpoint() mit Beschreibung ---");

        // checkpoint(description) fügt einen lesbaren Text in den Fehler-Stack ein.
        // checkpoint(description, true) erzeugt zusätzlich einen vollständigen Stack-Trace
        // beim Assembly-Zeitpunkt (teuer, aber sehr informativ).
        Flux<String> pipeline = Flux.just("alpha", "beta", null, "gamma")
            .checkpoint("nach-Quellenoperator")
            .map(String::toUpperCase)
            .checkpoint("nach-map-toUpperCase")
            .filter(s -> s.length() > 4);

        pipeline.subscribe(
            wert  -> System.out.println("  Wert: " + wert),
            fehler -> {
                System.out.println("  Fehler: " + fehler.getClass().getSimpleName());
                System.out.println("  (Checkpoint-Beschreibung im Stack sichtbar)");
            }
        );

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 6: Hooks.onOperatorDebug() – globales Assembly-Tracing
    // -------------------------------------------------------------------------
    static void beispiel6_HooksOnOperatorDebug() {
        System.out.println("--- Beispiel 6: Hooks.onOperatorDebug() ---");
        System.out.println("  ACHTUNG: nur für Entwicklung, nicht in Produktion!\n");

        // Aktiviert globales Stack-Trace-Capturing beim Assembly aller Pipelines.
        // Ermöglicht exakte Fehlerortung in Stack-Traces, aber mit Laufzeitkosten.
        Hooks.onOperatorDebug();

        Flux<Integer> flux = Flux.just(10, 5, 0, 2)
            .map(n -> 100 / n);

        flux.subscribe(
            wert  -> System.out.println("  Wert: " + wert),
            fehler -> {
                System.out.println("  Fehler: " + fehler.getMessage());
                System.out.println("  (Mit Hooks.onOperatorDebug() ist Assembly-Ort sichtbar)");
            }
        );

        // Hook wieder deaktivieren (sonst gilt er für alle nachfolgenden Pipelines!)
        Hooks.resetOnOperatorDebug();
        System.out.println("  Hooks.onOperatorDebug() wieder deaktiviert.");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 7: doOnEach() – alle Signale in einem Callback
    // -------------------------------------------------------------------------
    static void beispiel7_DoOnEach() throws InterruptedException {
        System.out.println("--- Beispiel 7: doOnEach() ---");

        // doOnEach() liefert jedes Signal (onNext, onError, onComplete) als
        // Signal<T>-Objekt. So können alle Signaltypen in einem einzigen
        // Lambda behandelt werden – praktisch für Logging und Metriken.
        Flux.range(1, 5)
            .doOnEach((Signal<Integer> signal) -> {
                if (signal.isOnNext()) {
                    System.out.println("  [doOnEach] onNext:    " + signal.get());
                } else if (signal.isOnError()) {
                    System.out.println("  [doOnEach] onError:   " + signal.getThrowable().getMessage());
                } else if (signal.isOnComplete()) {
                    System.out.println("  [doOnEach] onComplete");
                }
            })
            .filter(n -> n % 2 == 0)
            .subscribe(n -> System.out.println("  Subscriber: " + n));

        // Hinweis: Signal enthält auch den Context – nützlich zur Korrelation:
        System.out.println("\n  doOnEach() mit Context-Zugriff:");
        Flux.just("X", "Y", "Z")
            .doOnEach(signal -> {
                if (signal.isOnNext()) {
                    String reqId = signal.getContextView()
                                        .getOrDefault("reqId", "???");
                    System.out.println("  [" + reqId + "] " + signal.get());
                }
            })
            .contextWrite(ctx -> ctx.put("reqId", "REQ-99"))
            .subscribe();

        System.out.println();
    }
}
