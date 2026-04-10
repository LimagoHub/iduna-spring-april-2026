package de.bufferwindowgroup;

import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Modul 13: Buffer, Window und GroupBy
 *
 * Diese drei Operatoren gruppieren Elemente eines Flux auf unterschiedliche Weise:
 *
 *   buffer()   – sammelt Elemente in Listen (List<T>) und emittiert die Listen
 *                → Flux<T>  wird zu  Flux<List<T>>
 *
 *   window()   – öffnet für jede Gruppe einen neuen Flux (Flux<Flux<T>>)
 *                → Elemente können reaktiv weiterverarbeitet werden, bevor die Gruppe endet
 *
 *   groupBy()  – teilt einen Flux nach einem Schlüssel in GroupedFlux-Unterströme
 *                → Flux<T>  wird zu  Flux<GroupedFlux<K, T>>
 *                → jede Gruppe hat einen Schlüssel (key())
 *
 * Varianten:
 *   buffer(int size)            – feste Anzahl pro Puffer
 *   buffer(Duration timespan)   – zeitbasierter Puffer
 *   buffer(int size, int skip)  – überlappende (skip < size) oder lückige (skip > size) Puffer
 *   window(int size)            – feste Anzahl pro Fenster
 *   window(Duration timespan)   – zeitbasiertes Fenster
 *   groupBy(keySelector)        – Schlüssel-Extraktor
 *   groupBy(keySelector, valueMapper) – mit Wert-Transformation
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Modul 13: Buffer, Window und GroupBy ===\n");

        beispiel1_BufferMitGroesse();
        beispiel2_BufferSkipUndUeberlappung();
        beispiel3_BufferMitZeit();
        beispiel4_Window();
        beispiel5_WindowMitZeit();
        beispiel6_GroupBy();
        beispiel7_GroupByMitAggregation();
    }

    // -------------------------------------------------------------------------
    // Beispiel 1: buffer(n) – feste Puffergrösse
    // -------------------------------------------------------------------------
    static void beispiel1_BufferMitGroesse() {
        System.out.println("--- Beispiel 1: buffer(n) ---");

        // buffer(3) sammelt je 3 Elemente in einer List und emittiert die Liste
        Flux.range(1, 10)
            .buffer(3)
            .subscribe(liste -> System.out.println("  Puffer: " + liste));
        // [1,2,3] [4,5,6] [7,8,9] [10]  ← letzter Puffer kann kürzer sein

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 2: buffer(size, skip) – überlappende und lückige Puffer
    // -------------------------------------------------------------------------
    static void beispiel2_BufferSkipUndUeberlappung() {
        System.out.println("--- Beispiel 2: buffer(size, skip) ---");

        // skip < size → überlappende Puffer (sliding window)
        System.out.println("  Überlappend (size=3, skip=1):");
        Flux.range(1, 6)
            .buffer(3, 1)
            .subscribe(liste -> System.out.println("    " + liste));
        // [1,2,3] [2,3,4] [3,4,5] [4,5,6] [5,6] [6]

        // skip > size → lückige Puffer (Elemente werden übersprungen)
        System.out.println("  Lückig (size=2, skip=3):");
        Flux.range(1, 9)
            .buffer(2, 3)
            .subscribe(liste -> System.out.println("    " + liste));
        // [1,2] [4,5] [7,8]  (jeweils 1 Element übersprungen)

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 3: buffer(Duration) – zeitbasierter Puffer
    // -------------------------------------------------------------------------
    static void beispiel3_BufferMitZeit() throws InterruptedException {
        System.out.println("--- Beispiel 3: buffer(Duration) ---");

        CountDownLatch latch = new CountDownLatch(1);

        // Emittiert alle 100ms ein Element, gepuffert in 300ms-Fenstern
        Flux.interval(Duration.ofMillis(100))
            .take(8)
            .buffer(Duration.ofMillis(300))
            .doOnComplete(latch::countDown)
            .subscribe(liste -> System.out.println("  Zeitpuffer: " + liste));

        latch.await();
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 4: window(n) – Flux von Flux
    // -------------------------------------------------------------------------
    static void beispiel4_Window() throws InterruptedException {
        System.out.println("--- Beispiel 4: window(n) ---");

        CountDownLatch latch = new CountDownLatch(1);

        // window() gibt Flux<Flux<T>> zurück.
        // Jedes innere Flux muss subscribed werden, sonst wird es ignoriert!
        Flux.range(1, 9)
            .window(3)
            .flatMap(fenster -> {
                // Jeden Fenster-Flux in eine Summe umwandeln
                return fenster
                    .reduce(0, Integer::sum)
                    .map(summe -> "Fenster-Summe: " + summe);
            })
            .doOnComplete(latch::countDown)
            .subscribe(System.out::println);

        latch.await();
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 5: window(Duration) – zeitbasiertes Fenster
    // -------------------------------------------------------------------------
    static void beispiel5_WindowMitZeit() throws InterruptedException {
        System.out.println("--- Beispiel 5: window(Duration) ---");

        CountDownLatch latch = new CountDownLatch(1);

        // Öffnet alle 250ms ein neues Fenster
        Flux.interval(Duration.ofMillis(80))
            .take(10)
            .window(Duration.ofMillis(250))
            .index()                         // (fensterIndex, fensterFlux) Paare
            .flatMap(indexedFenster ->
                indexedFenster.getT2()
                    .collectList()
                    .map(liste -> "Fenster #" + indexedFenster.getT1() + ": " + liste)
            )
            .doOnComplete(latch::countDown)
            .subscribe(System.out::println);

        latch.await();
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 6: groupBy() – nach Schlüssel trennen
    // -------------------------------------------------------------------------
    static void beispiel6_GroupBy() throws InterruptedException {
        System.out.println("--- Beispiel 6: groupBy() ---");

        CountDownLatch latch = new CountDownLatch(1);

        // Gruppiert Zahlen nach gerade/ungerade
        Flux.range(1, 8)
            .groupBy(n -> n % 2 == 0 ? "gerade" : "ungerade")
            .flatMap(gruppe ->
                // GroupedFlux hat einen key()
                gruppe.collectList()
                      .map(liste -> gruppe.key() + ": " + liste)
            )
            .doOnComplete(latch::countDown)
            .subscribe(System.out::println);

        latch.await();
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 7: groupBy() mit Aggregation – Wörter nach Anfangsbuchstabe
    // -------------------------------------------------------------------------
    static void beispiel7_GroupByMitAggregation() throws InterruptedException {
        System.out.println("--- Beispiel 7: groupBy() mit Aggregation ---");

        CountDownLatch latch = new CountDownLatch(1);

        Flux<String> woerter = Flux.just(
            "Apfel", "Banane", "Avocado", "Birne",
            "Ananas", "Brombeere", "Clementine"
        );

        // Gruppiert nach erstem Buchstaben, zählt Elemente pro Gruppe
        woerter
            .groupBy(wort -> wort.charAt(0))
            .flatMap(gruppe ->
                gruppe.count()
                      .map(anzahl -> "  '" + gruppe.key() + "': " + anzahl + " Wort(e)")
            )
            .sort()   // Ausgabe sortieren für reproduzierbare Reihenfolge
            .doOnComplete(latch::countDown)
            .subscribe(System.out::println);

        latch.await();

        // Bonus: groupBy mit valueMapper
        System.out.println("\n  Mit valueMapper (Kleinbuchstaben):");
        CountDownLatch latch2 = new CountDownLatch(1);

        Flux.just("Alpha", "Beta", "Gamma", "Delta")
            .groupBy(
                s -> s.length(),                  // Schlüssel: Wortlänge
                s -> s.toLowerCase()              // Wert: in Kleinbuchstaben
            )
            .flatMap(gruppe ->
                gruppe.collectList()
                      .map(liste -> "  Länge " + gruppe.key() + ": " + liste)
            )
            .doOnComplete(latch2::countDown)
            .subscribe(System.out::println);

        latch2.await();
        System.out.println();
    }
}
