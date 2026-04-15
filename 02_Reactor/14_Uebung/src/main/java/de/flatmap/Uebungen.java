package de.flatmap;

import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

public class Uebungen {

    // ---------------------------------------------------------------------------
    // Einstiegspunkt – wird aus Main.java aufgerufen.
    // f1 = Vornamen (vornamen.txt): John, Max, John Boy, Dschingis
    // f2 = Nachnamen (nachnamen.txt): Doe, Mustermann, Walton, Kahn
    // ---------------------------------------------------------------------------
    static void run(Flux<String> f1, Flux<String> f2) throws InterruptedException {
        System.out.println("=== Übungen: Stream-Kombination mit f1 und f2 ===\n");

        aufgabe1_Concat(f1, f2);
        aufgabe2_Merge(f1, f2);
        aufgabe3_ZipTuple(f1, f2);
        aufgabe4_ZipCombinator(f1, f2);
        aufgabe5_CombineLatest(f1, f2);
        aufgabe6_ConcatWith(f1, f2);
        aufgabe7_ZipWith(f1, f2);
        aufgabe8_ZipVsCombineLatest(f1, f2);
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 1 – concat(): Vornamen und Nachnamen sequenziell zusammenführen
    // ---------------------------------------------------------------------------
    // f1 (Vornamen) wird erst vollständig abonniert, dann f2 (Nachnamen).
    // Reihenfolge garantiert: John Max John Boy Dschingis Doe Mustermann Walton Kahn
    // ---------------------------------------------------------------------------
    static void aufgabe1_Concat(Flux<String> f1, Flux<String> f2) {
        System.out.println("--- Aufgabe 1: concat() – Vornamen dann Nachnamen ---");

        Flux.concat(f1, f2)
            .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → Reihenfolge garantiert: erst alle Vornamen, dann alle Nachnamen\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 2 – merge(): Vornamen und Nachnamen parallel zusammenführen
    // ---------------------------------------------------------------------------
    // Bei synchronen Datei-Streams kein timing-Unterschied sichtbar.
    // Mit delayElements() wird das parallele Verhalten deutlich:
    // f1 (100ms) und f2 (150ms) werden abwechselnd ausgegeben.
    // ---------------------------------------------------------------------------
    static void aufgabe2_Merge(Flux<String> f1, Flux<String> f2) throws InterruptedException {
        System.out.println("--- Aufgabe 2: merge() – parallel mit Verzögerung ---");

        Flux<String> langsameVornamen  = f1.delayElements(Duration.ofMillis(100));
        Flux<String> langsamereNachname = f2.delayElements(Duration.ofMillis(150));

        Flux.merge(langsameVornamen, langsamereNachname)
            .subscribe(s -> System.out.println("  " + s));

        Thread.sleep(800);
        System.out.println("  → Reihenfolge nach Ankunft: beide Streams laufen gleichzeitig\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 3 – zip() mit Tuple: Vorname + Nachname als Tuple2
    // ---------------------------------------------------------------------------
    // zip() kombiniert je ein Element aus f1 und f2 zu einem Tuple2<String, String>.
    // Zugriff: t.getT1() = Vorname, t.getT2() = Nachname
    // Ergebnis: (John, Doe), (Max, Mustermann), (John Boy, Walton), (Dschingis, Kahn)
    // ---------------------------------------------------------------------------
    static void aufgabe3_ZipTuple(Flux<String> f1, Flux<String> f2) {
        System.out.println("--- Aufgabe 3: zip() mit Tuple – Vorname + Nachname ---");

        Flux.zip(f1, f2)
            .subscribe(t -> System.out.printf("  (%s, %s)%n", t.getT1(), t.getT2()));

        System.out.println("  → zip() paart Elemente mit gleichem Index zu Tuple2\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 4 – zip() mit Combinator: Vollständige Namen bilden
    // ---------------------------------------------------------------------------
    // Statt Tuple direkt eine Combinator-Funktion übergeben → Flux<String>
    // ohne Zwischenschritt über Tuple.
    // Ergebnis: "John Doe", "Max Mustermann", "John Boy Walton", "Dschingis Kahn"
    // ---------------------------------------------------------------------------
    static void aufgabe4_ZipCombinator(Flux<String> f1, Flux<String> f2) {
        System.out.println("--- Aufgabe 4: zip() mit Combinator – vollständige Namen ---");

        Flux.zip(f1, f2, (vorname, nachname) -> vorname + " " + nachname)
            .subscribe(name -> System.out.println("  " + name));

        System.out.println("  → Combinator ersetzt das Tuple durch einen direkten Ergebniswert\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 5 – combineLatest(): Immer neueste Kombination bei jeder Emission
    // ---------------------------------------------------------------------------
    // f1 (100ms): John → Max → John Boy → Dschingis
    // f2 (250ms): Doe → Mustermann → ...
    // Jede neue Emission aus einer Quelle kombiniert sich mit dem letzten
    // bekannten Wert der anderen Quelle → mehr Ausgaben als zip().
    // ---------------------------------------------------------------------------
    static void aufgabe5_CombineLatest(Flux<String> f1, Flux<String> f2) throws InterruptedException {
        System.out.println("--- Aufgabe 5: combineLatest() – immer neueste Kombination ---");

        Flux<String> vornamen  = f1.delayElements(Duration.ofMillis(100));
        Flux<String> nachnamen = f2.delayElements(Duration.ofMillis(250));

        Flux.combineLatest(vornamen, nachnamen,
                (v, n) -> v + " " + n)
            .subscribe(s -> System.out.println("  " + s));

        Thread.sleep(1500);
        System.out.println("  → jede neue Emission aus einer Quelle triggert eine neue Ausgabe\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 6 – concatWith(): Instanzmethode statt statischer Methode
    // ---------------------------------------------------------------------------
    // f1.concatWith(f2) ist identisch zu Flux.concat(f1, f2),
    // aber eleganter in einer Operator-Kette (fluent API).
    // ---------------------------------------------------------------------------
    static void aufgabe6_ConcatWith(Flux<String> f1, Flux<String> f2) {
        System.out.println("--- Aufgabe 6: concatWith() – Instanzmethode ---");

        f1.concatWith(f2)
          .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → concatWith() = Flux.concat(), aber direkt in der Kette\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 7 – zipWith(): Instanzmethode mit Combinator
    // ---------------------------------------------------------------------------
    // f1.zipWith(f2, combinator) ist identisch zu Flux.zip(f1, f2, combinator).
    // Ermöglicht das Einbauen einer zweiten Quelle mitten in einer Operator-Kette,
    // z.B. nach einem map().
    // ---------------------------------------------------------------------------
    static void aufgabe7_ZipWith(Flux<String> f1, Flux<String> f2) {
        System.out.println("--- Aufgabe 7: zipWith() – Instanzmethode in der Kette ---");

        f1.map(String::toUpperCase)
          .zipWith(f2, (vorname, nachname) -> nachname + ", " + vorname)
          .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → zipWith() fügt f2 direkt nach dem map() in die Kette ein\n");
    }

    // ---------------------------------------------------------------------------
    // Aufgabe 8 – zip() vs. combineLatest(): Direkter Vergleich mit Timing
    // ---------------------------------------------------------------------------
    // f1 (100ms): John → Max → John Boy → Dschingis
    // f2 (300ms): Doe → Mustermann → Walton → Kahn  ← langsamer
    //
    // zip():
    //   wartet auf je ein neues Paar → 4 Ausgaben, Tempo der langsameren Quelle
    //
    // combineLatest():
    //   reagiert auf jede einzelne Emission mit dem letzten Wert der anderen Quelle
    //   → mehr Ausgaben, früheres Reagieren auf neue Vornamen
    // ---------------------------------------------------------------------------
    static void aufgabe8_ZipVsCombineLatest(Flux<String> f1, Flux<String> f2) throws InterruptedException {
        System.out.println("--- Aufgabe 8: zip() vs. combineLatest() – Vergleich ---");

        System.out.println("  zip() – paarweise, Tempo der langsameren Quelle:");
        Flux.zip(
                f1.delayElements(Duration.ofMillis(100)),
                f2.delayElements(Duration.ofMillis(300)),
                (v, n) -> v + " " + n)
            .subscribe(s -> System.out.println("    " + s));

        Thread.sleep(1800);

        System.out.println("  combineLatest() – bei jeder Emission, letzter bekannter Wert:");
        Flux.combineLatest(
                f1.delayElements(Duration.ofMillis(100)),
                f2.delayElements(Duration.ofMillis(300)),
                (v, n) -> v + " " + n)
            .subscribe(s -> System.out.println("    " + s));

        Thread.sleep(1800);
        System.out.println("  → zip: gleiche Anzahl Paare | combineLatest: mehr Ausgaben, aktuellere Werte\n");
    }

    // ---------------------------------------------------------------------------
    // Hilfsmethode: neuen Flux aus Datei erzeugen (für mehrfache Nutzung)
    // ---------------------------------------------------------------------------
    static Flux<String> fromFile(String path) {
        return Flux.using(
            () -> Files.lines(Path.of(path)),
            Flux::fromStream,
            Stream::close
        );
    }
}
