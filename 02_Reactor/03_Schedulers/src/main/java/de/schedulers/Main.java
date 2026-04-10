package de.schedulers;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Reactor Schedulers ===\n");
        System.out.println("Hauptthread: " + Thread.currentThread().getName() + "\n");

        beispiel0_OhneScheduler();
        beispiel1_SubscribeOn();
        beispiel2_PublishOn();
        beispiel3_SubscribeOnUndPublishOn();
        beispiel4_ParalleleMono();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 0: Ohne Scheduler – Reactor ist NICHT automatisch asynchron
    // ---------------------------------------------------------------------------
    // Der häufigste Irrtum: "Reactor läuft automatisch auf einem anderen Thread."
    // Falsch. Ohne Scheduler läuft alles synchron im aufrufenden Thread –
    // genau wie normaler Java-Code.
    //
    // Das ist bewusst so: Reactor trennt die Beschreibung der Pipeline
    // (was passiert) von der Ausführungsstrategie (wo es passiert).
    // ---------------------------------------------------------------------------
    static void beispiel0_OhneScheduler() {
        System.out.println("--- Beispiel 0: Ohne Scheduler (synchron) ---");

        Flux.range(1, 3)
            .map(n -> {
                System.out.println("  map läuft auf:       " + Thread.currentThread().getName());
                return n * 2;
            })
            .subscribe(n ->
                System.out.println("  subscribe läuft auf: " + Thread.currentThread().getName())
            );

        System.out.println("  → alles auf 'main', synchron, nacheinander\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: subscribeOn – die Source auf einen anderen Thread verlegen
    // ---------------------------------------------------------------------------
    // subscribeOn() bestimmt, auf welchem Thread die Subscription startet –
    // also wo die Source ihre Elemente produziert.
    //
    // Das Besondere: subscribeOn() wirkt rückwärts durch die Pipeline.
    // Egal wo in der Kette es steht – es beeinflusst immer die Source.
    // Alle Operatoren VOR einem publishOn() laufen ebenfalls auf diesem Thread.
    //
    // Schedulers.boundedElastic() ist der richtige Scheduler für I/O-Operationen
    // (Datenbankzugriffe, HTTP-Calls, Dateioperationen) – ein elastischer
    // Thread-Pool, der bei Bedarf wächst, aber begrenzt bleibt.
    // ---------------------------------------------------------------------------
    static void beispiel1_SubscribeOn() {
        System.out.println("--- Beispiel 1: subscribeOn ---");
        System.out.println("  → Source und alle Operatoren laufen auf dem subscribeOn-Thread\n");

        Flux.range(1, 3)
            .map(n -> {
                System.out.println("  map läuft auf: " + Thread.currentThread().getName());
                return n * 2;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .blockLast(); // blockiert den Hauptthread bis der Stream abgeschlossen ist

        System.out.println("  → kein 'main' mehr – alles auf boundedElastic-Thread\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: publishOn – den Thread ab einem Punkt in der Pipeline wechseln
    // ---------------------------------------------------------------------------
    // publishOn() wechselt den Thread vorwärts ab der Stelle, wo es steht.
    // Alles vor publishOn() läuft auf dem bisherigen Thread,
    // alles danach auf dem neuen.
    //
    // Das erlaubt es, verschiedene Teile der Pipeline auf verschiedenen
    // Thread-Pools laufen zu lassen – z.B. I/O lesen auf boundedElastic,
    // dann auf parallel für CPU-intensive Transformation wechseln.
    //
    // Schedulers.parallel() ist für CPU-intensive Operationen:
    // ein fixer Pool mit N Threads (N = Anzahl CPU-Kerne).
    // ---------------------------------------------------------------------------
    static void beispiel2_PublishOn() {
        System.out.println("--- Beispiel 2: publishOn ---");
        System.out.println("  → Nur der Teil der Pipeline NACH publishOn wechselt den Thread\n");

        Flux.range(1, 3)
            .map(n -> {
                System.out.println("  [vor publishOn]  Thread: " + Thread.currentThread().getName());
                return n;
            })
            .publishOn(Schedulers.parallel())
            .map(n -> {
                System.out.println("  [nach publishOn] Thread: " + Thread.currentThread().getName());
                return n * 2;
            })
            .blockLast();

        System.out.println("  → vor publishOn: 'main', danach: parallel-Thread\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: subscribeOn + publishOn kombiniert
    // ---------------------------------------------------------------------------
    // Das typische Muster für I/O + CPU-Transformation:
    //
    //   subscribeOn(boundedElastic)  → Source liest Daten (z.B. aus DB/Datei)
    //   publishOn(parallel)          → CPU-intensive Verarbeitung der Daten
    //
    // Beide Wechsel passieren ohne blockierten Hauptthread.
    // ---------------------------------------------------------------------------
    static void beispiel3_SubscribeOnUndPublishOn() {
        System.out.println("--- Beispiel 3: subscribeOn + publishOn kombiniert ---");
        System.out.println("  → I/O-Thread für Source, CPU-Thread für Transformation\n");

        Flux.range(1, 3)
            .subscribeOn(Schedulers.boundedElastic())   // Source: I/O-Thread-Pool
            .map(n -> {
                System.out.println("  [Source/I/O]    Thread: " + Thread.currentThread().getName());
                return n;
            })
            .publishOn(Schedulers.parallel())           // ab hier: CPU-Thread-Pool
            .map(n -> {
                System.out.println("  [Transform/CPU] Thread: " + Thread.currentThread().getName());
                return n * 2;
            })
            .blockLast();

        System.out.println();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: Mehrere Mono-Operationen parallel ausführen
    // ---------------------------------------------------------------------------
    // flatMap() startet pro Element einen eigenen Sub-Publisher (hier: Mono).
    // Wenn jeder Mono auf Schedulers.boundedElastic() läuft, laufen alle
    // drei gleichzeitig – echter Parallelismus für I/O-Aufgaben.
    //
    // Mono.fromCallable() kapselt beliebigen Code (inkl. Exceptions) in einen Mono.
    // Hier simuliert Thread.sleep() eine I/O-Operation (DB, HTTP-Call etc.).
    //
    // Das ist das Grundmuster für parallele HTTP-Calls oder DB-Abfragen
    // in reaktiven Anwendungen.
    // ---------------------------------------------------------------------------
    static void beispiel4_ParalleleMono() {
        System.out.println("--- Beispiel 4: Parallele Mono-Aufgaben via flatMap ---");
        System.out.println("  → drei Aufgaben laufen gleichzeitig auf verschiedenen Threads\n");

        Flux.just("Aufgabe-A", "Aufgabe-B", "Aufgabe-C")
            .flatMap(aufgabe ->
                Mono.fromCallable(() -> {
                    System.out.println("  Start:  " + aufgabe + " auf Thread: " + Thread.currentThread().getName());
                    Thread.sleep(100); // simulierte I/O-Arbeit (z.B. DB-Abfrage)
                    System.out.println("  Fertig: " + aufgabe);
                    return aufgabe + " erledigt";
                }).subscribeOn(Schedulers.boundedElastic())
            )
            .blockLast();

        System.out.println("  → alle drei liefen gleichzeitig (nicht nacheinander)\n");
    }
}
