package de.testing;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

/**
 * TestPublisher ermöglicht vollständige Kontrolle über einen Publisher im Test.
 *
 * Wann braucht man TestPublisher?
 *   - Wenn man einen Operator oder eine Pipeline testen will, nicht den Publisher selbst
 *   - Wenn man den genauen Zeitpunkt von next/error/complete steuern muss
 *   - Wenn man prüfen will, wie der Subscriber auf bestimmte Signale reagiert
 *
 * TestPublisher ist der "manuelle Steuerhebel" für die Quelle eines Streams.
 * Man emittiert Elemente programmatisch – wann immer man es braucht.
 *
 * TestPublisher.create()         → normaler TestPublisher
 * TestPublisher.createNoncompliant(Violation...) → verletzt absichtlich
 *                                   das Reactive-Streams-Protokoll (z.B. null-Elemente)
 */
class TestPublisherBeispiele {

    ReactiveService service = new ReactiveService();

    // ---------------------------------------------------------------------------
    // Beispiel 1: Grundlagen – manuell Elemente emittieren
    // ---------------------------------------------------------------------------
    // TestPublisher.create() liefert einen Publisher, den man per:
    //   next(T...)     → onNext-Signal senden
    //   complete()     → onComplete-Signal senden
    //   error(Throwable) → onError-Signal senden
    //
    // Der StepVerifier abonniert den TestPublisher. Dann steuern wir
    // den Ablauf: erst Elemente senden, dann StepVerifier prüfen lassen.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel1_grundlagen() {
        TestPublisher<Integer> publisher = TestPublisher.create();

        StepVerifier.create(publisher.flux())
            .then(() -> publisher.next(10, 20, 30))   // Elemente emittieren
            .expectNext(10, 20, 30)
            .then(publisher::complete)                // Stream beenden
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 2: Fehler emittieren
    // ---------------------------------------------------------------------------
    // error(Throwable) sendet ein onError-Signal. Der StepVerifier
    // erwartet diesen Fehler mit expectError().
    //
    // Das ist besonders nützlich, wenn man testen will, wie eine Pipeline
    // auf Fehler reagiert (z.B. onErrorReturn, retry, ...).
    // ---------------------------------------------------------------------------
    @Test
    void beispiel2_fehlerEmittieren() {
        TestPublisher<String> publisher = TestPublisher.create();

        StepVerifier.create(publisher.flux())
            .then(() -> publisher.next("Wert A"))
            .expectNext("Wert A")
            .then(() -> publisher.error(new RuntimeException("Kaputt!")))
            .expectError(RuntimeException.class)
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 3: Einen Operator testen, nicht den Publisher
    // ---------------------------------------------------------------------------
    // Der eigentliche Nutzen von TestPublisher: Man testet eine Transformation
    // (einen Operator oder eine Service-Methode), während man die Quelle
    // vollständig kontrolliert.
    //
    // Hier testen wir ReactiveService.grossbuchstaben() unabhängig davon,
    // welche Quelle hinter der Methode steckt.
    // ---------------------------------------------------------------------------
    @Test
    void beispiel3_operatorTesten() {
        TestPublisher<String> quelle = TestPublisher.create();

        // Die Pipeline: quelle → grossbuchstaben()
        Flux<String> ergebnis = service.grossbuchstaben(quelle.flux());

        StepVerifier.create(ergebnis)
            .then(() -> quelle.next("hallo", "welt"))
            .expectNext("HALLO", "WELT")
            .then(quelle::complete)
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 4: Schritweise emittieren – Interleaving
    // ---------------------------------------------------------------------------
    // Mit .then() kann man zwischen den Expectations steuernd eingreifen.
    // Das erlaubt es, Elemente in genau dem Moment zu senden, in dem der
    // Subscriber bereit ist – wichtig für Backpressure-Tests.
    //
    // Ablauf hier:
    //   1. Subscriber meldet sich an
    //   2. Wir senden Element 1 → Subscriber prüft es
    //   3. Wir senden Element 2 → Subscriber prüft es
    //   4. Wir beenden den Stream
    // ---------------------------------------------------------------------------
    @Test
    void beispiel4_schrittweiseEmittieren() {
        TestPublisher<String> publisher = TestPublisher.create();

        StepVerifier.create(publisher.flux())
            .then(() -> publisher.next("Erster"))
            .expectNext("Erster")
            .then(() -> publisher.next("Zweiter"))
            .expectNext("Zweiter")
            .then(() -> publisher.next("Dritter"))
            .expectNext("Dritter")
            .then(publisher::complete)
            .expectComplete()
            .verify();
    }

    // ---------------------------------------------------------------------------
    // Beispiel 5: TestPublisher.assertWasCancelled / assertWasSubscribed
    // ---------------------------------------------------------------------------
    // TestPublisher merkt sich, was mit ihm passiert ist:
    //   assertWasSubscribed()   → wurde er abonniert?
    //   assertWasCancelled()    → wurde er abgebrochen?
    //   assertWasNotCancelled() → wurde er nicht abgebrochen?
    //
    // Nützlich wenn man prüfen will, ob eine Pipeline den Publisher
    // sauber kündigt (z.B. nach take() oder bei Fehlern).
    // ---------------------------------------------------------------------------
    @Test
    void beispiel5_assertions() {
        TestPublisher<Integer> publisher = TestPublisher.create();

        // take(2) kündigt den Publisher nach 2 Elementen
        StepVerifier.create(publisher.flux().take(2))
            .then(() -> publisher.next(1, 2, 3))
            .expectNext(1, 2)
            .expectComplete()
            .verify();

        publisher.assertWasSubscribed();
        publisher.assertWasCancelled();    // take() hat den Publisher abgebrochen
    }
}
