package de.testing;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Ein einfacher reaktiver Service – das ist der "Code unter Test".
 *
 * In echten Projekten wäre das z.B. ein Repository, ein HTTP-Client oder
 * ein Service, der Daten transformiert. Hier dient es nur als Grundlage
 * für die Test-Beispiele in StepVerifierBeispiele und TestPublisherBeispiele.
 */
public class ReactiveService {

    /** Liefert die Zahlen 1 bis n. */
    public Flux<Integer> zahlenBis(int n) {
        return Flux.range(1, n);
    }

    /** Liefert eine Liste von Wörtern. */
    public Flux<String> worte() {
        return Flux.just("Hallo", "Reaktiv", "Welt");
    }

    /** Liefert einen Wert – aber erst nach einer Verzögerung. */
    public Mono<String> langsameLadung() {
        return Mono.just("Ergebnis")
                   .delayElement(Duration.ofSeconds(3));
    }

    /** Emittiert zwei Werte, dann einen Fehler. */
    public Flux<Integer> fehlerNachZwei() {
        return Flux.concat(
            Flux.just(1, 2),
            Flux.error(new IllegalStateException("Etwas ist schiefgelaufen"))
        );
    }

    /** Wandelt Strings in Großbuchstaben um. */
    public Flux<String> grossbuchstaben(Flux<String> quelle) {
        return quelle.map(String::toUpperCase);
    }
}
