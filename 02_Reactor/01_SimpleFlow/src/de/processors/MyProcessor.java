package de.processors;

import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;

/**
 * Ein generischer Transformations-Processor im Reactive-Streams-Modell.
 *
 * <p>Ein {@link Processor} ist gleichzeitig {@link java.util.concurrent.Flow.Subscriber}
 * und {@link java.util.concurrent.Flow.Publisher}: Er empfängt Elemente eines Typs,
 * transformiert sie und leitet sie als anderen Typ weiter.</p>
 *
 * <p>Implementierungsstrategie: Statt {@link Processor} vollständig selbst zu
 * implementieren, erbt diese Klasse von {@link SubmissionPublisher}, der die
 * Publisher-Seite (Verwaltung von Subscribers, Thread-Handling, Backpressure-Puffer)
 * übernimmt. Wir implementieren nur die Subscriber-Seite manuell.</p>
 *
 * <p>In diesem Beispiel transformiert der Processor {@link String}-Elemente zu
 * {@link Integer}, indem er die Zeichenlängenfunktion {@link String#length()} anwendet.
 * Die konkrete Funktion wird über den Konstruktor injiziert, womit der Processor
 * generisch wiederverwendbar bleibt.</p>
 *
 * <pre>
 * Publisher&lt;String&gt;  →  MyProcessor&lt;String→Integer&gt;  →  Subscriber&lt;Integer&gt;
 * </pre>
 */
public class MyProcessor extends SubmissionPublisher<Integer>
        implements Processor<String, Integer> {

    /**
     * Die Subscription zum vorgelagerten Publisher.
     * Über sie wird Backpressure gesteuert: {@code subscription.request(n)}
     * fordert genau {@code n} weitere Elemente an.
     */
    private Subscription subscription;

    /**
     * Die Transformationsfunktion von String nach Integer.
     * Wird im Konstruktor übergeben – klassisches Strategy-Pattern.
     */
    private final Function<String, Integer> function;

    /**
     * Erstellt einen neuen Processor mit der angegebenen Transformationsfunktion.
     *
     * <p>Der Superklassen-Konstruktor {@link SubmissionPublisher#SubmissionPublisher()}
     * verwendet {@link java.util.concurrent.ForkJoinPool#commonPool()} als Executor
     * und einen Standard-Puffer für Backpressure.</p>
     *
     * @param function Transformationsfunktion String → Integer (darf nicht null sein)
     */
    public MyProcessor(final Function<String, Integer> function) {
        this.function = function;
    }

    /**
     * Handshake-Methode: Wird vom vorgelagerten Publisher aufgerufen,
     * sobald das Abonnement aktiv ist.
     *
     * <p>Wir speichern die Subscription und fordern sofort das erste Element an.
     * Ohne dieses {@code request(1)} würde der Publisher nichts senden –
     * das ist das Backpressure-Prinzip von Reactive Streams.</p>
     */
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1); // erstes Element vom Publisher anfordern
    }

    /**
     * Verarbeitungsschritt: Wird für jedes empfangene String-Element aufgerufen.
     *
     * <p>Die Transformationsfunktion wird angewendet und das Ergebnis mit
     * {@link SubmissionPublisher#submit(Object)} an nachgelagerte Subscriber
     * weitergeleitet. {@code submit()} kann blockieren, wenn ein Subscriber
     * seinen Puffer nicht schnell genug leert (Backpressure).</p>
     *
     * <p><b>Fehlerbehandlung:</b> Tritt bei der Transformation eine Exception auf,
     * wird sie geloggt und das fehlerhafte Element übersprungen. Das nächste
     * Element wird trotzdem angefordert ("skip and continue"-Strategie).
     * Alternativ wäre denkbar, den Stream über {@code subscription.cancel()}
     * abzubrechen oder den Fehler via {@link #closeExceptionally(Throwable)}
     * weiterzuleiten.</p>
     */
    @Override
    public void onNext(String item) {
        try {
            int result = function.apply(item);
            submit(result); // transformiertes Element an downstream Subscriber senden
        } catch (Exception e) {
            // Element überspringen, Fehler loggen, Verarbeitung fortsetzen
            System.err.println("Fehler bei Transformation von '" + item + "': " + e.getMessage());
        }
        // Nächstes Element anfordern – unabhängig davon, ob dieses erfolgreich war.
        // Das entspricht der "skip and continue"-Strategie.
        subscription.request(1);
    }

    /**
     * Fehlerbehandlung: Der vorgelagerte Publisher hat einen Fehler signalisiert.
     *
     * <p>Wir leiten den Fehler an alle downstream Subscriber weiter,
     * indem wir den {@link SubmissionPublisher} mit der Exception schließen.
     * Das löst bei allen Subscribern {@code onError()} aus.</p>
     */
    @Override
    public void onError(Throwable throwable) {
        this.closeExceptionally(throwable);
    }

    /**
     * Normale Terminierung: Der vorgelagerte Publisher hat alle Elemente gesendet.
     *
     * <p>Wir schließen den {@link SubmissionPublisher}, was bei allen downstream
     * Subscribern {@code onComplete()} auslöst. Damit propagiert das Complete-Signal
     * durch die gesamte Kette.</p>
     */
    @Override
    public void onComplete() {
        close(); // SubmissionPublisher schließen → löst onComplete() beim EndSubscriber aus
    }
}
