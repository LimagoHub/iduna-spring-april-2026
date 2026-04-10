package de.context;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Modul 11: Context
 *
 * Der Reactor-Context ist ein unveränderliches, schlüssel-basiertes
 * Datenwörterbuch, das sich "stromaufwärts" durch die Pipeline
 * ausbreitet – also entgegen der Datenfluss-Richtung.
 *
 * Wichtige Konzepte:
 *   contextWrite(ctx -> ctx.put(key, value))
 *     – Fügt Einträge in den Context ein (beim Subscriben, nicht beim Emittieren)
 *
 *   Mono.deferContextual(ctx -> ...)
 *   Flux.deferContextual(ctx -> ...)
 *     – Zugriff auf den Context beim Erzeugen der Pipeline (lazy)
 *
 *   ctx.read(key)      – Wert lesen (Optional)
 *   ctx.get(key)       – Wert lesen (Exception wenn nicht vorhanden)
 *   ctx.getOrDefault() – Wert lesen mit Fallback
 *
 * Wichtig: Context fließt ENTGEGEN dem Datenstrom.
 *   Der context-write()-Aufruf muss UNTERHALB der Stelle stehen,
 *   an der er gelesen wird (in Bezug auf die Operatorkette).
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Modul 11: Context ===\n");

        beispiel1_ContextSchreiben();
        beispiel2_ContextLesen();
        beispiel3_ContextPropagation();
        beispiel4_ContextImFlatMap();
        beispiel5_MehrereEintraege();
        beispiel6_ContextAktualisieren();
    }

    // -------------------------------------------------------------------------
    // Beispiel 1: Context schreiben und lesen
    // -------------------------------------------------------------------------
    static void beispiel1_ContextSchreiben() {
        System.out.println("--- Beispiel 1: Context schreiben und lesen ---");

        // deferContextual() erhält beim Subscribe den aktuellen Context
        Mono<String> pipeline = Mono.deferContextual(ctx -> {
            // ctx.getOrDefault() liefert Wert oder Default-Wert
            String benutzer = ctx.getOrDefault("benutzer", "Unbekannt");
            return Mono.just("Hallo, " + benutzer + "!");
        });

        // contextWrite() fügt Einträge ein – steht UNTERHALB des deferContextual()
        pipeline
            .contextWrite(Context.of("benutzer", "Anna"))
            .subscribe(System.out::println);  // "Hallo, Anna!"

        // Ohne contextWrite – Default-Wert wird verwendet
        pipeline
            .subscribe(System.out::println);  // "Hallo, Unbekannt!"

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 2: Context in Flux-Pipelines
    // -------------------------------------------------------------------------
    static void beispiel2_ContextLesen() {
        System.out.println("--- Beispiel 2: Context in Flux-Pipelines ---");

        Flux<String> flux = Flux.range(1, 3)
            .flatMap(i ->
                // deferContextual macht jeden Element-Schritt context-aware
                Mono.deferContextual(ctx -> {
                    String prefix = ctx.getOrDefault("prefix", "Item");
                    return Mono.just(prefix + "-" + i);
                })
            );

        flux
            .contextWrite(Context.of("prefix", "Produkt"))
            .subscribe(System.out::println);

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 3: Context-Propagation – Richtung beachten!
    // -------------------------------------------------------------------------
    static void beispiel3_ContextPropagation() {
        System.out.println("--- Beispiel 3: Context-Propagation (Richtung) ---");

        // RICHTIG: contextWrite() steht unterhalb (= nach Subscription-Richtung)
        // des deferContextual()-Aufrufs.
        Mono.deferContextual(ctx ->
                Mono.just("Wert aus Context: " + ctx.getOrDefault("schluessel", "fehlt"))
            )
            .contextWrite(Context.of("schluessel", "GEFUNDEN"))
            .subscribe(System.out::println);  // "Wert aus Context: GEFUNDEN"

        // FALSCH (Demo): contextWrite() steht oberhalb – der Context ist noch leer
        Mono.deferContextual(ctx ->
                Mono.just("Wert aus Context: " + ctx.getOrDefault("schluessel", "fehlt"))
            )
            .contextWrite(Context.of("anderer-schluessel", "xyz"))
            .subscribe(System.out::println);  // "Wert aus Context: fehlt"

        System.out.println("  → contextWrite() schreibt 'aufwärts' (entgegen dem Datenfluss)");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 4: Context im flatMap – Korrelations-ID weitergeben
    // -------------------------------------------------------------------------
    static void beispiel4_ContextImFlatMap() {
        System.out.println("--- Beispiel 4: Context im flatMap (Korrelations-ID) ---");

        // Typisches Muster: Request-ID aus dem Context in jeden API-Aufruf mitgeben
        Flux<String> pipeline = Flux.just("Nutzer-A", "Nutzer-B", "Nutzer-C")
            .flatMap(name ->
                Mono.deferContextual(ctx -> {
                    String requestId = ctx.getOrDefault("requestId", "???");
                    // Simuliert einen Service-Aufruf mit Korrelations-ID im Log
                    System.out.println("    [reqId=" + requestId + "] Verarbeite: " + name);
                    return Mono.just(name.toUpperCase());
                })
            );

        pipeline
            .contextWrite(Context.of("requestId", "REQ-4711"))
            .subscribe(ergebnis -> System.out.println("    Ergebnis: " + ergebnis));

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 5: Mehrere Context-Einträge und Schichtung
    // -------------------------------------------------------------------------
    static void beispiel5_MehrereEintraege() {
        System.out.println("--- Beispiel 5: Mehrere Einträge und Schichtung ---");

        // Context.of() für mehrere Einträge (bis zu 10 Schlüssel-Wert-Paare)
        Mono<String> pipeline = Mono.deferContextual(ctx -> {
            String sprache = ctx.getOrDefault("sprache", "de");
            String version = ctx.getOrDefault("version", "1.0");
            String benutzer = ctx.getOrDefault("benutzer", "Gast");
            return Mono.just(String.format("[%s v%s] Willkommen, %s!", sprache, version, benutzer));
        });

        pipeline
            .contextWrite(Context.of(
                "sprache", "EN",
                "version", "2.5",
                "benutzer", "Bob"
            ))
            .subscribe(System.out::println);

        // Schichtung: inneres contextWrite überschreibt äußeres für gleichnamige Schlüssel
        System.out.println("  Schichtung (inner überschreibt outer):");
        Mono.deferContextual(ctx ->
                Mono.just("Benutzer: " + ctx.getOrDefault("benutzer", "?"))
            )
            .contextWrite(Context.of("benutzer", "Outer-Clara"))  // wird IGNORIERT für "benutzer"
            .contextWrite(Context.of("benutzer", "Inner-Dave"))   // gewinnt (näher an der Quelle)
            .subscribe(System.out::println);

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Beispiel 6: Context aktualisieren
    // -------------------------------------------------------------------------
    static void beispiel6_ContextAktualisieren() {
        System.out.println("--- Beispiel 6: Context aktualisieren ---");

        // contextWrite nimmt eine Funktion, die den bisherigen Context erweitern kann
        Mono<String> pipeline = Mono.deferContextual(ctx ->
            Mono.just("Zähler: " + ctx.getOrDefault("zaehler", 0))
        );

        pipeline
            .contextWrite(ctx -> ctx.put("zaehler", 1))       // ersten Eintrag setzen
            .subscribe(System.out::println);  // "Zähler: 1"

        // Wert inkrementieren (Context ist immutable – put() gibt neuen Context zurück)
        Mono<String> inkrement = Mono.deferContextual(ctx -> {
            int aktuell = ctx.getOrDefault("zaehler", 0);
            return Mono.just("Alter Wert: " + aktuell);
        });

        inkrement
            .contextWrite(ctx -> ctx.put("zaehler", ctx.<Integer>get("zaehler") + 10))
            .contextWrite(ctx -> ctx.put("zaehler", 5))
            .subscribe(System.out::println);  // 5 + 10 = 15

        System.out.println();
    }
}
