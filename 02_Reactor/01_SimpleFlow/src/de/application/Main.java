package de.application;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

import de.processors.MyProcessor;
import de.subscriber.EndSubscriber;

/**
 * Einstiegspunkt für das Beispielprojekt zur Java Flow API (Reactive Streams).
 *
 * <p>Dieses Programm demonstriert eine dreistufige Reactive-Streams-Pipeline:</p>
 *
 * <pre>
 * SubmissionPublisher&lt;String&gt;
 *         ↓  (liefert Strings)
 * MyProcessor  (String → Integer via String::length)
 *         ↓  (liefert Integer)
 * EndSubscriber&lt;Integer&gt;  (gibt Werte auf der Konsole aus)
 * </pre>
 *
 * <p>Die Java Flow API ({@link java.util.concurrent.Flow}, seit Java 9) ist
 * eine standardisierte Implementierung der Reactive-Streams-Spezifikation
 * (https://www.reactive-streams.org). Sie definiert vier Kernschnittstellen:</p>
 * <ul>
 *   <li>{@link java.util.concurrent.Flow.Publisher}    – Datenquelle</li>
 *   <li>{@link java.util.concurrent.Flow.Subscriber}   – Datenempfänger</li>
 *   <li>{@link java.util.concurrent.Flow.Processor}    – Transformation (beides zugleich)</li>
 *   <li>{@link java.util.concurrent.Flow.Subscription} – Verbindung + Backpressure-Steuerung</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        // --- 1. Testdaten ---
        // Bewusst doppelte Liste, um zu zeigen, dass alle Elemente verarbeitet werden.
        // Die Integer-Ausgabe zeigt die Zeichenlänge jedes Strings.
        List<String> worte = List.of("1", "2", "drei", "4", "fünf",
                                     "1", "2", "drei", "4", "fünf");

        // --- 2. Pipeline aufbauen (Reihenfolge: von hinten nach vorne!) ---
        //
        // Wichtig: Die Subscriber müssen registriert sein, BEVOR der Publisher
        // Daten sendet. Daher wird die Kette von Ende nach Anfang aufgebaut.

        // Endverbraucher: gibt empfangene Integer aus
        EndSubscriber<Integer> endSubscriber = new EndSubscriber<>();

        // Processor: transformiert String → Integer (hier: Zeichenlänge)
        // MyProcessor erbt von SubmissionPublisher und implementiert Processor<String, Integer>
        MyProcessor processor = new MyProcessor(String::length);

        // Processor → EndSubscriber verbinden (Processor als Publisher, EndSubscriber als Subscriber)
        processor.subscribe(endSubscriber);

        // Publisher: SubmissionPublisher ist die einzige fertige Publisher-Implementierung im JDK.
        // Der parameterlose Konstruktor verwendet ForkJoinPool.commonPool() als Executor
        // und verarbeitet Items asynchron in einem separaten Thread-Pool.
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        // Publisher → Processor verbinden
        publisher.subscribe(processor);

        // --- 3. Daten senden ---
        // submit() übergibt jedes Element an den Publisher.
        // Bei gefülltem Puffer blockiert submit() den sendenden Thread (Backpressure).
        worte.forEach(publisher::submit);

        // --- 4. Stream abschließen ---
        // close() signalisiert dem Publisher, dass keine weiteren Elemente folgen.
        // Dies löst asynchron onComplete() im Processor und im EndSubscriber aus.
        publisher.close();

        // --- 5. Auf Abschluss warten ---
        //
        // WARUM NICHT executor.shutdown() + awaitTermination()?
        // SubmissionPublisher verwendet intern ForkJoinPool.commonPool(). Dieser
        // gemeinsame Pool kann NICHT heruntergefahren werden – shutdown() ist dort
        // ein No-op. awaitTermination() würde deshalb bis zum Timeout blockieren.
        //
        // LÖSUNG: CountDownLatch im EndSubscriber. onComplete() zählt den Latch
        // auf 0, awaitCompletion() blockiert den Main-Thread bis dahin.
        // Das ist idiomatisch, korrekt und ohne Timing-Hacks.
        endSubscriber.awaitCompletion();

        System.out.println("Alle Elemente verarbeitet. Programm beendet.");
    }
}
