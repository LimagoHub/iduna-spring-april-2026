package de.flatmap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== flatMap – der wichtigste Reaktiv-Operator ===\n");

        beispiel1_MapVsFlatMap();
        beispiel2_FlatMapMitMono();
        beispiel3_FlatMapParallel();
        beispiel4_ConcatMap();
        beispiel5_SwitchMap();
        beispiel6_FlatMapIterable();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: map vs. flatMap
    // ---------------------------------------------------------------------------
    // map()     transformiert jeden Wert 1:1 synchron → T → R
    // flatMap() transformiert jeden Wert in einen Publisher → T → Publisher<R>
    //           und "flattet" (verkettet) die Ergebnisse in einen einzigen Stream.
    //
    // Wann flatMap nötig ist: wenn die Transformation selbst asynchron ist,
    // d.h. wenn sie einen Mono oder Flux zurückgibt (z.B. DB-Abfrage, HTTP-Call).
    // ---------------------------------------------------------------------------
    static void beispiel1_MapVsFlatMap() {
        System.out.println("--- Beispiel 1: map vs. flatMap ---");

        // map: synchrone 1:1-Transformation – gibt direkt den Wert zurück
        System.out.println("  map (synchron, 1:1):");
        Flux.just(1, 2, 3)
            .map(n -> n * 10)                        // int → int
            .subscribe(n -> System.out.println("    " + n));

        System.out.println();

        // flatMap: jeder Wert wird zu einem Publisher expandiert
        // Hier: jede Zahl erzeugt einen Flux mit zwei Elementen (n und n+1)
        System.out.println("  flatMap (1:n, expandiert):");
        Flux.just(1, 2, 3)
            .flatMap(n -> Flux.just(n * 10, n * 10 + 1))  // int → Flux<int>
            .subscribe(n -> System.out.println("    " + n));

        System.out.println("  → map: 3 rein, 3 raus  /  flatMap: 3 rein, 6 raus\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: flatMap für asynchrone Operationen (Mono)
    // ---------------------------------------------------------------------------
    // Das häufigste Muster in reaktiven Anwendungen:
    // Für jedes Element eine asynchrone Operation starten (DB, HTTP) und
    // das Ergebnis wieder in den Stream einspeisen.
    //
    // Mono.fromCallable() kapselt beliebigen (potenziell blockierenden) Code.
    // Ohne subscribeOn läuft er noch synchron – subscribeOn verlegt ihn auf
    // einen anderen Thread-Pool.
    // ---------------------------------------------------------------------------
    static void beispiel2_FlatMapMitMono() {
        System.out.println("--- Beispiel 2: flatMap mit Mono (asynchrone Operation) ---");

        Flux.just("Alice", "Bob", "Carol")
            .flatMap(name -> ladeBenutzerprofil(name))  // String → Mono<String>
            .subscribe(profil -> System.out.println("  " + profil));

        System.out.println();
    }

    // Simuliert eine asynchrone DB-Abfrage oder einen HTTP-Call
    static Mono<String> ladeBenutzerprofil(String name) {
        return Mono.fromCallable(() -> "Profil[" + name + ", Rolle=Admin]");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: flatMap für echten Parallelismus
    // ---------------------------------------------------------------------------
    // flatMap() abonniert alle Sub-Publisher sofort – nicht nacheinander.
    // Wenn jeder Mono auf Schedulers.boundedElastic() läuft, laufen alle
    // gleichzeitig auf verschiedenen Threads.
    //
    // Das ist das Grundmuster für parallele HTTP-Calls:
    //   Ohne flatMap: 3 × 200ms = 600ms
    //   Mit flatMap:  max(200ms, 200ms, 200ms) ≈ 200ms
    // ---------------------------------------------------------------------------
    static void beispiel3_FlatMapParallel() {
        System.out.println("--- Beispiel 3: flatMap – echte parallele Ausführung ---");

        long start = System.currentTimeMillis();

        Flux.just("Service-A", "Service-B", "Service-C")
            .flatMap(service ->
                Mono.fromCallable(() -> {
                    System.out.println("  → Anfrage an " + service
                            + " auf Thread: " + Thread.currentThread().getName());
                    Thread.sleep(200); // simulierter HTTP-Call
                    return service + ": OK";
                }).subscribeOn(Schedulers.boundedElastic())
            )
            .blockLast();

        long dauer = System.currentTimeMillis() - start;
        System.out.println("  Gesamtdauer: " + dauer + "ms  (3 × 200ms parallel ≈ 200ms)\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: concatMap – geordnete Alternative zu flatMap
    // ---------------------------------------------------------------------------
    // flatMap()   → startet alle Sub-Publisher sofort, Reihenfolge unbestimmt
    // concatMap() → wartet auf Abschluss des vorherigen, Reihenfolge garantiert
    //
    // Wann concatMap verwenden?
    // - Wenn die Reihenfolge wichtig ist (z.B. Datenbank-Inserts in Sequenz)
    // - Wenn nachfolgende Operationen von vorherigen abhängen
    // - Wenn gleichzeitige Ausführung Probleme macht (Race Conditions)
    //
    // Nachteil: Kein Parallelismus – läuft wie ein for-each, nur reaktiv.
    // ---------------------------------------------------------------------------
    static void beispiel4_ConcatMap() {
        System.out.println("--- Beispiel 4: concatMap – reihenfolgeerhaltend ---");

        System.out.println("  concatMap: wartet auf Abschluss, Reihenfolge garantiert:");
        Flux.just("Schritt-1", "Schritt-2", "Schritt-3")
            .concatMap(schritt ->
                Mono.fromCallable(() -> {
                    System.out.println("  Start:  " + schritt);
                    Thread.sleep(100);
                    System.out.println("  Fertig: " + schritt);
                    return schritt + " erledigt";
                }).subscribeOn(Schedulers.boundedElastic())
            )
            .blockLast();

        System.out.println("  → Schritt-1 → Schritt-2 → Schritt-3 (immer in dieser Reihenfolge)\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: switchMap – nur das neueste Element zählt
    // ---------------------------------------------------------------------------
    // switchMap() bricht den laufenden Sub-Publisher ab, sobald ein neues
    // Element eintrifft, und startet einen neuen.
    //
    // Klassisches Beispiel: Typeahead-Suche.
    // Wenn der Nutzer schnell tippt, interessiert nur die letzte Eingabe.
    // Alte HTTP-Requests werden abgebrochen – spart Ressourcen.
    //
    // Hier simuliert durch flatMap mit Verzögerung und einem schnelleren Wert:
    // (switchMap würde nur das Ergebnis des letzten Elements ausgeben)
    // ---------------------------------------------------------------------------
    static void beispiel5_SwitchMap() {
        System.out.println("--- Beispiel 5: switchMap – nur letzter Wert zählt ---");

        // switchMap: der zweite Wert bricht den ersten Sub-Publisher ab
        Flux.just("re", "rea", "reac", "react")
            .switchMap(eingabe ->
                Mono.fromCallable(() -> {
                    System.out.println("  Suche gestartet für: '" + eingabe + "'");
                    Thread.sleep(150);
                    return "Ergebnis für: '" + eingabe + "'";
                }).subscribeOn(Schedulers.boundedElastic())
            )
            .blockLast();

        System.out.println("  → Nur das Ergebnis des letzten aktiven Publishers kommt an.\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: flatMapIterable – Listen in Streams auflösen
    // ---------------------------------------------------------------------------
    // flatMapIterable() ist der synchrone Spezialfall von flatMap:
    // Jedes Element wird zu einem Iterable (List, Set, ...) expandiert.
    //
    // Häufiger Use-Case: Eine Liste von Bestellungen, jede Bestellung enthält
    // mehrere Positionen – man will alle Positionen in einem einzigen Stream.
    // ---------------------------------------------------------------------------
    static void beispiel6_FlatMapIterable() {
        System.out.println("--- Beispiel 6: flatMapIterable – Listen expandieren ---");

        Flux.just(
                List.of("Apfel", "Birne"),
                List.of("Kirsche", "Mango", "Pflaume"),
                List.of("Orange")
        )
        .flatMapIterable(liste -> liste)   // List<String> → einzelne Strings
        .subscribe(frucht -> System.out.println("  " + frucht));

        System.out.println("  → 3 Listen rein, 6 einzelne Strings raus\n");
    }
}
