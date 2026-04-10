package de.streamcombination;

import reactor.core.publisher.Flux;

import java.time.Duration;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Stream-Kombination ===\n");

        beispiel1_Concat();
        beispiel2_Merge();
        beispiel3_ConcatVsMerge();
        beispiel4_Zip();
        beispiel5_CombineLatest();
        beispiel6_MergeWith();
        beispiel7_ZipWith();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: concat() – sequenziell, Reihenfolge garantiert
    // ---------------------------------------------------------------------------
    // concat() abonniert den zweiten Publisher erst, wenn der erste abgeschlossen
    // ist. Die Reihenfolge der Elemente entspricht der Reihenfolge der Quellen.
    //
    // Wann verwenden?
    //   - Wenn Reihenfolge wichtig ist (z.B. Seiten 1, 2, 3 nacheinander laden)
    //   - Wenn Quelle 2 von den Ergebnissen von Quelle 1 abhängen könnte
    //   - Wenn die Quellen nicht gleichzeitig laufen sollen
    //
    // Achtung: Wenn Quelle 1 nie abschließt (z.B. ein unendlicher Flux),
    // wird Quelle 2 niemals abonniert!
    // ---------------------------------------------------------------------------
    static void beispiel1_Concat() {
        System.out.println("--- Beispiel 1: concat() – sequenziell ---");

        Flux<String> quelle1 = Flux.just("A", "B", "C");
        Flux<String> quelle2 = Flux.just("X", "Y", "Z");

        Flux.concat(quelle1, quelle2)
            .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → Reihenfolge garantiert: A B C X Y Z\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: merge() – parallel, Reihenfolge nach Ankunft
    // ---------------------------------------------------------------------------
    // merge() abonniert alle Quellen gleichzeitig und leitet Elemente weiter,
    // sobald sie ankommen – unabhängig von der Reihenfolge der Quellen.
    //
    // Wann verwenden?
    //   - Wenn mehrere unabhängige Quellen parallel laufen
    //   - Wenn Durchsatz wichtiger ist als Reihenfolge
    //   - Typisch: mehrere WebSocket-Streams oder Event-Quellen zusammenführen
    //
    // Hier mit Verzögerungen, damit man den Unterschied zu concat() sieht.
    // ---------------------------------------------------------------------------
    static void beispiel2_Merge() throws InterruptedException {
        System.out.println("--- Beispiel 2: merge() – parallel, nach Ankunft ---");

        Flux<String> schnell = Flux.just("schnell-1", "schnell-2")
            .delayElements(Duration.ofMillis(100));

        Flux<String> langsam = Flux.just("langsam-1", "langsam-2")
            .delayElements(Duration.ofMillis(250));

        Flux.merge(schnell, langsam)
            .subscribe(s -> System.out.println("  " + s));

        Thread.sleep(700);
        System.out.println("  → Reihenfolge nach Ankunft: schnell-1, schnell-2, langsam-1, ...\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: concat() vs. merge() – direkter Vergleich ohne Async
    // ---------------------------------------------------------------------------
    // Bei synchronen Quellen (ohne delay) ist der Unterschied weniger sichtbar,
    // weil Quellen sofort abschließen. Daher hier ein einfacher Vergleich, der
    // zeigt, dass concat() die Quellen nacheinander, merge() sie "gleichzeitig"
    // abonniert – bei synchronen Quellen mit identischem Ergebnis.
    //
    // Der echte Unterschied zeigt sich erst bei zeitbasierten Quellen (Beispiel 2).
    // ---------------------------------------------------------------------------
    static void beispiel3_ConcatVsMerge() {
        System.out.println("--- Beispiel 3: concat() vs. merge() – synchroner Vergleich ---");

        Flux<Integer> a = Flux.range(1, 3);
        Flux<Integer> b = Flux.range(10, 3);

        System.out.print("  concat(): ");
        Flux.concat(a, b).subscribe(n -> System.out.print(n + " "));
        System.out.println();

        System.out.print("  merge():  ");
        Flux.merge(a, b).subscribe(n -> System.out.print(n + " "));
        System.out.println();

        System.out.println("  → synchron: kein Unterschied; bei async: merge() mischt\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: zip() – paarweises Kombinieren
    // ---------------------------------------------------------------------------
    // zip() wartet auf ein Element von JEDER Quelle und kombiniert sie zu einem
    // Tupel (oder per Combinator-Funktion). Der langsamste Publisher bestimmt
    // das Tempo. Wenn eine Quelle beendet ist, stoppt der gesamte zip()-Flux.
    //
    // Wann verwenden?
    //   - Wenn Elemente aus mehreren Quellen zusammengehören (gleicher Index)
    //   - Typisch: Vorname-Liste + Nachname-Liste → vollständige Namen
    //   - API-Ergebnis + Metadaten-Ergebnis → kombiniertes Objekt
    //
    // zip() mit 2 Quellen: Tuple2<T1, T2>
    // zip() mit Combinator: eigene Klasse / String etc.
    // ---------------------------------------------------------------------------
    static void beispiel4_Zip() {
        System.out.println("--- Beispiel 4: zip() – paarweise kombinieren ---");

        Flux<String> vornamen   = Flux.just("Anna", "Ben", "Clara");
        Flux<String> nachnamen  = Flux.just("Müller", "Schmidt", "Weber");
        Flux<Integer> alter     = Flux.just(30, 25, 35);

        // zip() mit 2 Quellen → Tuple2
        System.out.println("  zip(vornamen, nachnamen):");
        Flux.zip(vornamen, nachnamen)
            .subscribe(t -> System.out.println("    " + t.getT1() + " " + t.getT2()));

        // zip() mit 3 Quellen → Tuple3
        System.out.println("  zip(vornamen, nachnamen, alter) → Tuple3:");
        Flux.zip(
                Flux.just("Anna", "Ben", "Clara"),
                Flux.just("Müller", "Schmidt", "Weber"),
                alter
            )
            .subscribe(t -> System.out.println("    " + t.getT1() + " " + t.getT2() + " (" + t.getT3() + ")"));

        System.out.println("  → immer gleich viele Elemente: kürzeste Quelle stoppt\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: combineLatest() – immer das neueste Paar
    // ---------------------------------------------------------------------------
    // combineLatest() kombiniert immer das NEUESTE Element jeder Quelle.
    // Sobald eine neue Emission kommt, wird sie mit dem letzten bekannten
    // Wert der anderen Quellen kombiniert.
    //
    // Wann verwenden?
    //   - UI-Formulare: Wert aus Feld A + Wert aus Feld B → Validierung
    //   - Live-Konfiguration: neue Einstellung + aktueller Zustand → Reaktion
    //   - Sensordaten: Temperatur-Sensor + Luftdruck-Sensor → kombinierter Alarm
    //
    // Unterschied zu zip():
    //   zip()           → wartet auf neue Elemente aus ALLEN Quellen (paarlweise)
    //   combineLatest() → reagiert auf jede einzelne Emission mit dem letzten Stand
    // ---------------------------------------------------------------------------
    static void beispiel5_CombineLatest() throws InterruptedException {
        System.out.println("--- Beispiel 5: combineLatest() – immer neuestes Paar ---");

        Flux<String> temperatur = Flux.just("20°C", "22°C", "25°C")
            .delayElements(Duration.ofMillis(100));

        Flux<String> luftdruck = Flux.just("1013 hPa", "1010 hPa")
            .delayElements(Duration.ofMillis(200));

        Flux.combineLatest(temperatur, luftdruck,
                (t, l) -> t + " / " + l)
            .subscribe(s -> System.out.println("  " + s));

        Thread.sleep(700);
        System.out.println("  → jede neue Emission kombiniert mit dem letzten anderen Wert\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: concatWith() / mergeWith() – Instanzmethoden
    // ---------------------------------------------------------------------------
    // Neben den statischen Methoden Flux.concat() und Flux.merge() gibt es
    // Instanzmethoden: flux1.concatWith(flux2) und flux1.mergeWith(flux2).
    //
    // Sie sind identisch in der Funktion, aber syntaktisch angenehmer bei
    // Methodenverkettung (fluent API).
    // ---------------------------------------------------------------------------
    static void beispiel6_MergeWith() {
        System.out.println("--- Beispiel 6: concatWith() / mergeWith() – Instanzmethoden ---");

        Flux<Integer> a = Flux.just(1, 2, 3);
        Flux<Integer> b = Flux.just(4, 5, 6);

        System.out.print("  concatWith(): ");
        a.concatWith(b).subscribe(n -> System.out.print(n + " "));
        System.out.println();

        System.out.print("  mergeWith():  ");
        Flux.just(1, 2, 3).mergeWith(Flux.just(4, 5, 6))
            .subscribe(n -> System.out.print(n + " "));
        System.out.println();

        System.out.println("  → identisch zu Flux.concat() / Flux.merge(), nur als Instanzmethode\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 7: zipWith() – Instanzmethode + Unterschied zu zip()
    // ---------------------------------------------------------------------------
    // zipWith() ist die Instanzmethode zu Flux.zip().
    // Nützlich beim Verketten von Operatoren, ohne eine neue statische Methode
    // aufrufen zu müssen.
    //
    // Typisches Muster:
    //   Wenn man das Ergebnis eines vorherigen Operators direkt mit einer
    //   zweiten Quelle kombinieren möchte, ohne die Kette zu unterbrechen.
    // ---------------------------------------------------------------------------
    static void beispiel7_ZipWith() {
        System.out.println("--- Beispiel 7: zipWith() – Instanzmethode ---");

        Flux.just("Montag", "Dienstag", "Mittwoch")
            .zipWith(Flux.range(1, 3),
                (tag, nr) -> nr + ". " + tag)
            .subscribe(s -> System.out.println("  " + s));

        System.out.println("  → Instanzmethode: direkt in die Operator-Kette eingebaut\n");
    }
}
