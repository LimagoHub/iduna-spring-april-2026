package de.errorhandling;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Error Handling in Project Reactor ===\n");

        beispiel1_FehlerOhneBehandlung();
        beispiel2_OnErrorReturn();
        beispiel3_OnErrorResume();
        beispiel4_OnErrorMap();
        beispiel5_FehlerInFlatMap();
        beispiel6_Retry();
        beispiel7_RetryWhen();
        beispiel8_DoOnError();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: Was passiert ohne Fehlerbehandlung?
    // ---------------------------------------------------------------------------
    // Tritt in einem Flux ein Fehler auf, wird der Stream sofort abgebrochen.
    // Alle Elemente nach dem Fehler gehen verloren.
    // Der Fehler landet im onError-Handler des Subscribers.
    //
    // Das ist Reactive Streams Spec: ein Stream endet entweder mit
    // onComplete() oder onError() – danach kommt nichts mehr.
    // ---------------------------------------------------------------------------
    static void beispiel1_FehlerOhneBehandlung() {
        System.out.println("--- Beispiel 1: Fehler ohne Behandlung ---");

        Flux.just(1, 2, 3, 4, 5)
            .map(n -> {
                if (n == 3) throw new RuntimeException("Fehler bei Element 3!");
                return n;
            })
            .subscribe(
                n   -> System.out.println("  Element: " + n),
                err -> System.out.println("  FEHLER:  " + err.getMessage()),
                ()  -> System.out.println("  Abgeschlossen")   // wird NICHT erreicht
            );

        System.out.println("  → Stream abgebrochen, Elemente 4 und 5 nie geliefert\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: onErrorReturn – Fallback-Wert
    // ---------------------------------------------------------------------------
    // onErrorReturn() fängt den Fehler ab und liefert stattdessen
    // einen statischen Fallback-Wert. Danach ist der Stream normal beendet.
    //
    // Wann verwenden?
    // - Wenn ein sinnvoller Standardwert existiert (0, leere Liste, "unbekannt")
    // - Wenn der Fehlerfall kein showstopper ist
    // ---------------------------------------------------------------------------
    static void beispiel2_OnErrorReturn() {
        System.out.println("--- Beispiel 2: onErrorReturn – Fallback-Wert ---");

        Flux.just("Alice", "Bob", null, "Carol")
            .map(name -> name.toUpperCase())          // NullPointerException bei null
            .onErrorReturn("UNBEKANNT")               // Fallback statt Fehler
            .subscribe(
                name -> System.out.println("  " + name),
                err  -> System.out.println("  Fehler: " + err.getMessage())
            );

        System.out.println("  → kein Crash, Stream endet sauber mit Fallback-Wert\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: onErrorResume – Fallback-Publisher
    // ---------------------------------------------------------------------------
    // onErrorResume() ersetzt den fehlerhaften Stream durch einen anderen Publisher.
    // Der Subscriber merkt den Fehler nicht – er sieht nur den Fallback-Stream.
    //
    // Mächtiger als onErrorReturn, weil der Fallback selbst asynchron sein kann:
    // z.B. Cache lesen, wenn die DB nicht erreichbar ist.
    // ---------------------------------------------------------------------------
    static void beispiel3_OnErrorResume() {
        System.out.println("--- Beispiel 3: onErrorResume – Fallback-Publisher ---");

        Flux<String> hauptQuelle = Flux.just("Live-Daten A", "Live-Daten B")
            .concatWith(Flux.error(new RuntimeException("Verbindung verloren")));

        Flux<String> fallbackQuelle = Flux.just("[Cache] Daten A", "[Cache] Daten B");

        hauptQuelle
            .onErrorResume(err -> {
                System.out.println("  Fehler abgefangen: " + err.getMessage());
                System.out.println("  Wechsle auf Cache-Daten...");
                return fallbackQuelle;
            })
            .subscribe(s -> System.out.println("  " + s));

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: onErrorMap – Fehler transformieren
    // ---------------------------------------------------------------------------
    // onErrorMap() konvertiert einen technischen Fehler in einen
    // domänenspezifischen Fehler. Der Stream bricht trotzdem ab –
    // nur der Fehlertyp ändert sich.
    //
    // Wichtig für Schichtenarchitektur: Die Service-Schicht soll keine
    // technischen Exceptions (SQLException, IOException) nach oben durchreichen.
    // ---------------------------------------------------------------------------
    static void beispiel4_OnErrorMap() {
        System.out.println("--- Beispiel 4: onErrorMap – Fehlertyp transformieren ---");

        Mono.fromCallable(() -> {
                throw new java.sql.SQLException("Verbindung zur DB getrennt");
            })
            .onErrorMap(java.sql.SQLException.class,
                ex -> new RuntimeException("Datenbankfehler: " + ex.getMessage(), ex))
            .subscribe(
                v   -> System.out.println("  Ergebnis: " + v),
                err -> System.out.println("  Domänenfehler: " + err.getMessage())
            );

        System.out.println("  → technische Exception in Domänen-Exception umgewandelt\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: Fehler in flatMap – Isolation vs. Propagation
    // ---------------------------------------------------------------------------
    // Kritisch zu verstehen: Ein Fehler in einem Sub-Publisher von flatMap
    // propagiert standardmäßig nach oben und bricht den gesamten Stream ab.
    //
    // Um einzelne Sub-Publisher fehlertolerant zu machen, muss der Fehler
    // INNERHALB des Sub-Publishers behandelt werden – vor der flatMap.
    //
    // Das ist der häufigste Fehler bei parallelen API-Calls:
    // Ein fehlschlagender Call bricht alle anderen ab.
    // ---------------------------------------------------------------------------
    static void beispiel5_FehlerInFlatMap() {
        System.out.println("--- Beispiel 5: Fehlerbehandlung in flatMap ---");

        System.out.println("  Ohne Isolation: ein Fehler bricht alle ab:");
        Flux.just("Service-A", "Service-B", "Service-C")
            .flatMap(service ->
                rufeDienstAuf(service)
                    .onErrorReturn(service + ": FEHLER (isoliert)")  // Fehler im Sub-Publisher behandeln
            )
            .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → Service-B scheitert, aber A und C laufen durch\n");
    }

    static Mono<String> rufeDienstAuf(String service) {
        if (service.equals("Service-B")) {
            return Mono.error(new RuntimeException("Service-B nicht erreichbar"));
        }
        return Mono.just(service + ": OK");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: retry – automatisches Wiederholen
    // ---------------------------------------------------------------------------
    // retry(n) wiederholt die gesamte Subscription n-mal, wenn ein Fehler auftritt.
    // Nützlich bei transienten Fehlern (kurze Netzwerkstörungen, DB-Timeouts).
    //
    // Achtung: retry() resubscribed die gesamte Source – bei Cold Publishers
    // (z.B. HTTP-Request) bedeutet das: der Request wird erneut gesendet.
    // Bei teuren Operationen lieber retryWhen() mit Backoff verwenden.
    // ---------------------------------------------------------------------------
    static void beispiel6_Retry() {
        System.out.println("--- Beispiel 6: retry – automatisches Wiederholen ---");

        AtomicInteger versuch = new AtomicInteger(0);

        Mono.fromCallable(() -> {
                int v = versuch.incrementAndGet();
                System.out.println("  Versuch " + v + "...");
                if (v < 3) throw new RuntimeException("Temporärer Fehler");
                return "Erfolg nach " + v + " Versuchen";
            })
            .retry(3)   // bis zu 3 Wiederholungen
            .subscribe(
                s   -> System.out.println("  " + s),
                err -> System.out.println("  Endgültig fehlgeschlagen: " + err.getMessage())
            );

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 7: retryWhen – intelligentes Retry mit Backoff
    // ---------------------------------------------------------------------------
    // retryWhen() erlaubt vollständige Kontrolle über die Retry-Strategie:
    // - Wie lange warten? (exponentieller Backoff)
    // - Wie oft maximal?
    // - Nur bei bestimmten Fehlertypen?
    //
    // Reactor.util.retry.Retry bietet fertige Builder dafür.
    // Exponentieller Backoff ist der Standard in produktiven Anwendungen –
    // er verhindert, dass ein überlasteter Service mit Anfragen überflutet wird.
    // ---------------------------------------------------------------------------
    static void beispiel7_RetryWhen() {
        System.out.println("--- Beispiel 7: retryWhen – Backoff-Strategie ---");

        AtomicInteger versuch = new AtomicInteger(0);

        Mono.fromCallable(() -> {
                int v = versuch.incrementAndGet();
                System.out.println("  Versuch " + v + " auf Thread: "
                        + Thread.currentThread().getName());
                if (v < 3) throw new RuntimeException("Service überlastet");
                return "Erfolg";
            })
            .retryWhen(
                Retry.backoff(3, Duration.ofMillis(100))   // max. 3 Versuche, Start: 100ms Pause
                     .maxBackoff(Duration.ofMillis(500))   // maximale Pause: 500ms
                     .jitter(0.5)                          // zufällige Streuung: ±50%
            )
            .block();  // blockiert für Demo (in echter App nicht verwenden)

        System.out.println("  → exponentieller Backoff schützt den überlasteten Service\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 8: doOnError – Logging ohne den Fehler zu behandeln
    // ---------------------------------------------------------------------------
    // doOnError() ist ein Side-Effect-Operator: er reagiert auf Fehler
    // (z.B. für Logging oder Metriken), verändert aber den Stream nicht.
    // Der Fehler propagiert weiter – im Gegensatz zu onErrorReturn/Resume.
    //
    // Kombination mit onErrorResume: erst loggen, dann erholen.
    // Das ist das Standardmuster für Fehler-Observability in reaktiven Apps.
    // ---------------------------------------------------------------------------
    static void beispiel8_DoOnError() {
        System.out.println("--- Beispiel 8: doOnError – Logging + Behandlung kombinieren ---");

        Mono.fromCallable(() -> {
                throw new RuntimeException("Unerwarteter Fehler");
            })
            .doOnError(err ->
                System.out.println("  [LOG] Fehler aufgetreten: " + err.getMessage()
                        + " (wird geloggt, aber weiter propagiert)")
            )
            .onErrorResume(err -> Mono.just("Fallback-Wert"))
            .subscribe(s -> System.out.println("  Subscriber erhält: " + s));

        System.out.println("  → Fehler wurde geloggt UND der Stream hat sich erholt\n");
    }
}
