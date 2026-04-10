package de.testing;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * StepVerifier ist das zentrale Werkzeug zum Testen reaktiver Streams in Reactor.
 *
 * Grundprinzip:
 *   1. StepVerifier.create(publisher) → Verifikation aufbauen
 *   2. .expectNext(...)              → erwartete Elemente festlegen
 *   3. .expectComplete()             → erwartetes Ende festlegen
 *   4. .verify()                     → Stream abonnieren und prüfen
 *
 * Ohne .verify() läuft der Test NICHT – das ist ein häufiger Fehler!
 *
 * Wichtig: StepVerifier abonniert den Publisher blockierend.
 * verify() blockiert, bis der Stream abgeschlossen ist oder ein Timeout greift.
 */
class StepVerifierBeispiele {

    ReactiveService service = new ReactiveService();

    // ---------------------------------------------------------------------------
    // Beispiel 1: Grundlagen – expectNext und expectComplete
    // ---------------------------------------------------------------------------
    // Die häufigste Form: einzelne Werte prüfen, dann Abschluss erwarten.
    //
    // expectNext(T...)  → prüft exakt diese Werte in dieser Reihenfolge
    // expectComplete()  → erwartet das onComplete-Signal
    // verify()          → startet den Test (blockierend)
    //
    // Fehler: Stimmt ein Wert nicht, schlägt der Test mit einer AssertionError-
    // Nachricht fehl, die zeigt was erwartet und was empfangen wurde.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel1_grundlagen() {
        Flux<Integer> zahlen = service.zahlenBis(3);

        StepVerifier.create(zahlen)
            .expectNext(1)
            .expectNext(2)
            .expectNext(3)
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: Mehrere Werte auf einmal prüfen
    // ---------------------------------------------------------------------------
    // expectNext(T...) kann mehrere Werte gleichzeitig prüfen.
    // expectNextCount(n) prüft, dass genau n weitere Elemente folgen,
    // ohne deren Inhalt zu inspizieren. Nützlich bei langen Streams.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel2_mehrereWerteUndCount() {
        // Alle Werte auf einmal prüfen
        StepVerifier.create(service.worte())
            .expectNext("Hallo", "Reaktiv", "Welt")
            .expectComplete()
            .verify();

        // Nur die Anzahl prüfen (Inhalt ignorieren)
        StepVerifier.create(service.zahlenBis(100))
            .expectNextCount(100)
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: Fehler erwarten – expectError
    // ---------------------------------------------------------------------------
    // Wenn ein Stream mit onError endet, muss das explizit erwartet werden.
    //
    // expectError()                    → irgendein Fehler
    // expectError(Class<?>)            → Fehler vom bestimmten Typ
    // expectErrorMessage(String)       → Fehler mit bestimmter Nachricht
    // expectErrorMatches(Predicate)    → Fehler mit eigener Prüflogik
    //
    // Wichtig: Nach expectError() darf kein expectComplete() mehr kommen –
    // ein Stream endet entweder mit Complete ODER Error, nie mit beidem.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel3_fehlerErwarten() {
        Flux<Integer> fehlerhaft = service.fehlerNachZwei();

        StepVerifier.create(fehlerhaft)
            .expectNext(1, 2)
            .expectError(IllegalStateException.class)
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: Eigene Assertions – expectNextMatches und assertNext
    // ---------------------------------------------------------------------------
    // Manchmal reicht einfaches Gleichheitsvergleich nicht aus.
    //
    // expectNextMatches(Predicate<T>)  → Prädikat muss true zurückgeben
    // assertNext(Consumer<T>)          → beliebige Assertions im Consumer
    //                                    (z.B. mit AssertJ oder JUnit)
    // ---------------------------------------------------------------------------
    @Test
    void beispiel4_eigeneAssertions() {
        StepVerifier.create(service.zahlenBis(5))
            .expectNextMatches(n -> n == 1)
            .expectNextMatches(n -> n % 2 == 0)      // 2 ist gerade
            .assertNext(n -> {
                // Hier kann man beliebige JUnit-Assertions verwenden
                assert n == 3 : "Erwartet 3, aber war: " + n;
            })
            .expectNextCount(2)                       // 4 und 5 nur zählen
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: Mono testen
    // ---------------------------------------------------------------------------
    // Mono<T> hat entweder genau ein Element oder keines.
    //
    // Für ein leeres Mono: expectComplete() direkt nach create()
    // Für ein Mono mit Wert: expectNext(wert).expectComplete()
    //
    // verifyComplete()  = expectComplete().verify()    (Kurzform)
    // verifyError(...)  = expectError(...).verify()    (Kurzform)
    // ---------------------------------------------------------------------------
    @Test
    void beispiel5_monoTesten() {
        // Mono mit Wert
        Mono<String> mono = Mono.just("Hallo");

        StepVerifier.create(mono)
            .expectNext("Hallo")
            .verifyComplete();    // Kurzform für .expectComplete().verify()

        // Leeres Mono
        Mono<String> leer = Mono.empty();

        StepVerifier.create(leer)
            .verifyComplete();    // kein expectNext – kein Element erwartet

        // Mono mit Fehler
        Mono<String> fehler = Mono.error(new RuntimeException("Fehler!"));

        StepVerifier.create(fehler)
            .verifyError(RuntimeException.class);   // Kurzform für expectError + verify
    }

    // ---------------------------------------------------------------------------
    // Beispiel 6: Virtuelle Zeit – zeitbasierte Operatoren ohne Warten
    // ---------------------------------------------------------------------------
    // Operatoren wie delayElement(), interval() oder timeout() würden in
    // echten Tests sekundenlang blockieren. Mit VirtualTimeScheduler kann
    // Reactor die Zeit "vorspulen": thenAwait(duration) simuliert den
    // Zeitablauf ohne tatsächlich zu warten.
    //
    // withVirtualTime(() -> publisher)
    //     Der Publisher MUSS innerhalb des Lambdas erstellt werden –
    //     nicht vorher! Sonst ist der Scheduler noch nicht eingehängt.
    //
    // thenAwait(Duration)  → virtuelle Zeit um diesen Wert vorspulen
    // expectNoEvent(Duration) → prüfen, dass in dieser Zeit NICHTS passiert
    // ---------------------------------------------------------------------------
    @Test
    void beispiel6_virtualzeit() {
        // Ohne VirtualTime würde dieser Test 3 Sekunden dauern!
        StepVerifier.withVirtualTime(() -> service.langsameLadung())
            .expectSubscription()
            .expectNoEvent(Duration.ofSeconds(2))   // in 2s passiert nichts
            .thenAwait(Duration.ofSeconds(1))        // restliche Zeit vorspulen
            .expectNext("Ergebnis")
            .verifyComplete();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 7: verifyThenAssertThat – Statistiken nach dem Test
    // ---------------------------------------------------------------------------
    // Nach verify() kann man mit verifyThenAssertThat() zusätzliche
    // Aussagen über den abgelaufenen Stream machen:
    //   - Wie viele Elemente wurden fallen gelassen?
    //   - Wurden Elemente discarded?
    //
    // Nützlich zum Testen von Backpressure-Strategien.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel7_statistikenNachTest() {
        StepVerifier.create(service.zahlenBis(5))
            .expectNext(1, 2, 3, 4, 5)
            .expectComplete()
            .verifyThenAssertThat()
            .hasNotDroppedElements()
            .hasNotDroppedErrors();
    }
}
