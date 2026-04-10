package de.hotcold;

import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Hot vs. Cold Publishers ===\n");

        beispiel1_ColdPublisher();
        beispiel2_HotPublisherMitInterval();
        beispiel3_PublishUndConnect();
        beispiel4_Autoconnect();
        beispiel5_Share();
        beispiel6_Cache();
        beispiel7_Sinks();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 1: Cold Publisher – jeder Subscriber bekommt die volle Sequenz
    // ---------------------------------------------------------------------------
    // Ein Cold Publisher startet die Datenproduktion NEU für jeden Subscriber.
    // Das ist das Standardverhalten in Reactor:
    //
    //   Flux.just(...)            → Cold
    //   Flux.range(...)           → Cold
    //   Flux.fromIterable(...)    → Cold
    //   Mono.fromCallable(...)    → Cold
    //
    // Analogie: Eine DVD. Jeder, der sie einlegt, sieht den Film von Anfang an.
    //
    // Wichtige Konsequenz: Bei HTTP-Calls oder DB-Abfragen als Cold Publisher
    // wird die Operation für jeden Subscriber ERNEUT ausgeführt!
    // ---------------------------------------------------------------------------
    static void beispiel1_ColdPublisher() {
        System.out.println("--- Beispiel 1: Cold Publisher – jeder Subscriber von Anfang ---");

        Flux<Integer> cold = Flux.range(1, 3)
            .map(n -> {
                System.out.println("  produziere: " + n);
                return n;
            });

        System.out.println("  Subscriber 1:");
        cold.subscribe(n -> System.out.println("    Sub1 erhält: " + n));

        System.out.println("  Subscriber 2:");
        cold.subscribe(n -> System.out.println("    Sub2 erhält: " + n));

        System.out.println("  → 'produziere' erscheint 6× – für jeden Subscriber neu\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: Hot Publisher – alle Subscriber teilen denselben Stream
    // ---------------------------------------------------------------------------
    // Ein Hot Publisher produziert Elemente unabhängig davon, ob jemand zuhört.
    // Subscribers, die erst später einsteigen, verpassen frühere Elemente.
    //
    //   Flux.interval(...)        → Hot (Ticker läuft weiter)
    //
    // Analogie: Ein Radiosender. Wer später einschaltet, hört nur den aktuellen
    // Stand des Programms – den Anfang hat er verpasst.
    //
    // Hier gezeigt: Subscriber 2 steigt 200ms später ein und verpasst
    // die ersten Elemente.
    // ---------------------------------------------------------------------------
    static void beispiel2_HotPublisherMitInterval() throws InterruptedException {
        System.out.println("--- Beispiel 2: Hot Publisher – Flux.interval (Ticker) ---");

        // Flux.interval ist von Natur aus "hot-ähnlich": er läuft auf einem
        // eigenen Scheduler und hört nicht auf, wenn kein Subscriber da ist.
        // Hier: 2 Subscriber, der zweite steigt 150ms später ein.
        Flux<Long> ticker = Flux.interval(Duration.ofMillis(100))
            .take(5);  // nur 5 Elemente für die Demo

        ticker.subscribe(n -> System.out.println("  Sub1 erhält: " + n));
        Thread.sleep(250);  // Sub2 steigt 250ms später ein → verpasst Elemente 0 und 1
        ticker.subscribe(n -> System.out.println("                       Sub2 erhält: " + n));

        Thread.sleep(400);  // warten bis beide fertig sind

        System.out.println("  → Sub2 beginnt bei 0 (Cold-Verhalten von interval + take)");
        System.out.println("  → für echtes Hot-Sharing: publish() verwenden (Beispiel 3)\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: publish() + connect() – Cold in Hot umwandeln
    // ---------------------------------------------------------------------------
    // publish() erzeugt einen ConnectableFlux: er startet die Produktion ERST,
    // wenn connect() aufgerufen wird.
    //
    // So kann man mehrere Subscriber registrieren, bevor der Stream losläuft –
    // alle teilen sich dann denselben laufenden Stream.
    //
    // Ablauf:
    //   1. publish()       → ConnectableFlux erstellen (noch kein Start)
    //   2. subscribe(...)  → Subscriber registrieren
    //   3. connect()       → Produktion startet, alle Subscriber erhalten Elemente
    // ---------------------------------------------------------------------------
    static void beispiel3_PublishUndConnect() throws InterruptedException {
        System.out.println("--- Beispiel 3: publish() + connect() – Cold → Hot ---");

        ConnectableFlux<Integer> connectable = Flux.range(1, 4)
            .map(n -> {
                System.out.println("  produziere: " + n);
                return n;
            })
            .publish();  // noch kein Start

        connectable.subscribe(n -> System.out.println("    Sub1 erhält: " + n));
        connectable.subscribe(n -> System.out.println("    Sub2 erhält: " + n));

        System.out.println("  → beide Subscriber registriert, noch keine Produktion");
        System.out.println("  → jetzt connect():");
        connectable.connect();  // Produktion startet – beide Subscriber erhalten alle Elemente

        System.out.println("  → 'produziere' erscheint 4× (nicht 8×) – geteilt\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: autoConnect() – automatisch verbinden bei erstem Subscriber
    // ---------------------------------------------------------------------------
    // autoConnect(n) verbindet automatisch, sobald n Subscriber vorhanden sind.
    // Kein manuelles connect() nötig.
    //
    // autoConnect(1): startet mit dem ersten Subscriber.
    //   → Praktisch, aber Achtung: spätere Subscriber verpassen frühere Elemente.
    //
    // autoConnect(2): wartet auf 2 Subscriber, dann Start.
    //   → Beide erhalten alle Elemente, wenn sie sich rechtzeitig anmelden.
    // ---------------------------------------------------------------------------
    static void beispiel4_Autoconnect() {
        System.out.println("--- Beispiel 4: autoConnect() – automatisch verbinden ---");

        Flux<Integer> autoHot = Flux.range(1, 4)
            .publish()
            .autoConnect(2);  // startet bei 2 Subscribern

        System.out.println("  Subscriber 1 meldet sich an (wartet auf 2. Subscriber)...");
        autoHot.subscribe(n -> System.out.println("    Sub1: " + n));

        System.out.println("  Subscriber 2 meldet sich an → Produktion startet:");
        autoHot.subscribe(n -> System.out.println("    Sub2: " + n));

        System.out.println("  → beide erhalten alle Elemente 1–4\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: share() – multicast an aktive Subscriber
    // ---------------------------------------------------------------------------
    // share() ist eine Kurzform für publish().refCount():
    //
    //   - Startet automatisch, sobald der erste Subscriber da ist
    //   - Beendet die Upstream-Subscription, wenn der letzte Subscriber geht
    //   - Startet neu, wenn wieder ein neuer Subscriber kommt (Cold-Restart)
    //
    // Wann verwenden?
    // - Wenn mehrere Teile der Anwendung denselben Datenstrom abonnieren
    //   und die Quelle teuer ist (HTTP-Poll, WebSocket)
    // - Subscriber, die zu spät kommen, verpassen frühere Elemente
    // ---------------------------------------------------------------------------
    static void beispiel5_Share() {
        System.out.println("--- Beispiel 5: share() – refCount-basiertes Multicast ---");

        Flux<Integer> shared = Flux.range(1, 4)
            .map(n -> {
                System.out.println("  produziere: " + n);
                return n;
            })
            .share();  // publish().refCount()

        shared.subscribe(n -> System.out.println("    Sub1: " + n));
        shared.subscribe(n -> System.out.println("    Sub2: " + n));

        System.out.println("  → 'produziere' erscheint 4× (nicht 8×)\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: cache() – Ergebnisse für zukünftige Subscriber aufbewahren
    // ---------------------------------------------------------------------------
    // cache() speichert die emittierten Elemente und spielt sie jedem neuen
    // Subscriber sofort ab – auch wenn der ursprüngliche Stream schon beendet ist.
    //
    // cache() vs. share():
    //   share()  → kein Replay, späte Subscriber verpassen Elemente
    //   cache()  → Replay, späte Subscriber erhalten alle gecachten Elemente
    //
    // Typischer Use-Case: Konfiguration oder Initialisierungsdaten, die einmal
    // geladen und danach mehrfach verwendet werden (z.B. ein HTTP-Aufruf, dessen
    // Ergebnis nicht jedes Mal neu geholt werden soll).
    // ---------------------------------------------------------------------------
    static void beispiel6_Cache() {
        System.out.println("--- Beispiel 6: cache() – Replay für späte Subscriber ---");

        Flux<Integer> cached = Flux.range(1, 3)
            .map(n -> {
                System.out.println("  produziere (einmalig): " + n);
                return n;
            })
            .cache();  // Ergebnisse einmal produzieren, dann wiederverwenden

        System.out.println("  Erster Subscriber:");
        cached.subscribe(n -> System.out.println("    Sub1: " + n));

        System.out.println("  Zweiter Subscriber (stream schon beendet):");
        cached.subscribe(n -> System.out.println("    Sub2: " + n));

        System.out.println("  → 'produziere' erscheint nur 3× – Sub2 bekommt Cache-Replay\n");
    }

    // ---------------------------------------------------------------------------
    // Beispiel 7: Sinks – programmatisch in einen Stream emittieren
    // ---------------------------------------------------------------------------
    // Sinks sind die Reactor-Entsprechung eines programmatischen Publishers:
    // Man kann von außen Elemente in den Stream schieben.
    //
    // Sinks.many().multicast() – Hot Stream, mehrere Subscriber
    //   → Jeder Subscriber erhält alle Elemente, die nach seiner Subscription kommen
    //
    // Sinks.one() – wie ein einmaliges Versprechen (ähnlich CompletableFuture)
    //
    // Wann Sinks verwenden?
    // - Event-getriebene Systeme (UI-Events, WebSocket-Nachrichten)
    // - Wenn man von imperativen Code in einen reaktiven Stream emittieren will
    //   (z.B. aus einem Callback heraus)
    // ---------------------------------------------------------------------------
    static void beispiel7_Sinks() {
        System.out.println("--- Beispiel 7: Sinks – programmatisch emittieren ---");

        // Multicast-Sink: alle aktuellen Subscriber erhalten die Elemente
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<String> stream = sink.asFlux();

        stream.subscribe(s -> System.out.println("  Sub1: " + s));
        stream.subscribe(s -> System.out.println("  Sub2: " + s));

        System.out.println("  → emittiere 'Hallo':");
        sink.tryEmitNext("Hallo");

        System.out.println("  → emittiere 'Welt':");
        sink.tryEmitNext("Welt");

        sink.tryEmitComplete();

        System.out.println("  → beide Subscriber haben alle Elemente erhalten\n");
    }
}
