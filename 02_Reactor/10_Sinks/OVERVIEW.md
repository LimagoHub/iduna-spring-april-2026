# Modul 10: Sinks

## Lernziele

- Verstehen, was ein Sink ist und warum er gebraucht wird
- `Sinks.One<T>` für einen einzelnen Wert verwenden
- `Sinks.Many<T>` mit den Strategien Unicast, Multicast und Replay einsetzen
- Thread-sicheres Emittieren mit `tryEmit*()` und `emitNext()`
- `EmitResult`-Werte auswerten und auf Fehler reagieren

---

## Was ist ein Sink?

Ein **Sink** ist ein programmatischer Publisher: Er ermöglicht es, imperativ
(z.B. aus einem Callback, Event-Listener oder einem anderen Thread) Elemente
in eine reaktive Pipeline einzuspeisen.

```
imperativer Code          reaktive Pipeline
──────────────────         ──────────────────────────────
sink.tryEmitNext("A")  →   Flux<String>  →  Subscriber
sink.tryEmitNext("B")
sink.tryEmitComplete()
```

Typische Anwendungsfälle:
- WebSocket-Nachrichten in einen Flux einspeisen
- Ergebnisse aus einem Callback-basierten System reaktiv weitergeben
- Tests: gesteuerte Emissionen simulieren

---

## Sinks.One vs. Sinks.Many

| Eigenschaft           | `Sinks.One<T>`              | `Sinks.Many<T>`               |
|-----------------------|-----------------------------|-------------------------------|
| Anzahl Elemente       | genau 1 (oder Fehler)       | beliebig viele                |
| Entspricht            | `Mono<T>`                   | `Flux<T>`                     |
| Subscriber            | mehrere möglich (Replay)    | abhängig von Strategie        |
| Late-Subscriber       | ja (gecachter Wert)         | nur bei `replay()`            |

---

## Sinks.Many – Strategien

| Strategie                              | Subscriber | Replay | Backpressure-Handling          |
|----------------------------------------|-----------|--------|-------------------------------|
| `unicast().onBackpressureBuffer()`     | genau 1   | nein   | Puffer bis Subscriber kommt   |
| `multicast().directBestEffort()`       | mehrere   | nein   | Elemente gehen verloren, wenn kein Sub. |
| `multicast().onBackpressureBuffer()`   | mehrere   | nein   | Puffer                        |
| `replay().all()`                       | mehrere   | alles  | unbegrenzter Puffer           |
| `replay().limit(n)`                    | mehrere   | n      | Ringpuffer der Größe n        |
| `replay().limit(Duration)`             | mehrere   | zeitl. | Zeitfenster-Puffer            |

---

## tryEmit* vs. emitNext/emitValue

```java
// tryEmit*: nicht-blockierend, gibt EmitResult zurück
Sinks.EmitResult result = sink.tryEmitNext(wert);
if (result.isFailure()) {
    // Fehler behandeln
}

// emitNext: wirft bei Fehler eine Exception (mit EmitFailureHandler)
sink.emitNext(wert, Sinks.EmitFailureHandler.FAIL_FAST);

// Thread-sichere Retry-Schleife:
Sinks.EmitResult r;
do {
    r = sink.tryEmitNext(wert);
} while (r == Sinks.EmitResult.FAIL_NON_SERIALIZED);
```

---

## EmitResult-Werte

| Wert                       | Bedeutung                                            |
|----------------------------|------------------------------------------------------|
| `OK`                       | Emission erfolgreich                                 |
| `FAIL_TERMINATED`          | Sink ist bereits abgeschlossen                       |
| `FAIL_ALREADY_TERMINATED`  | Complete oder Error wurde bereits gesendet           |
| `FAIL_OVERFLOW`            | Backpressure-Puffer ist voll                         |
| `FAIL_ZERO_SUBSCRIBER`     | Kein Subscriber vorhanden (bei directBestEffort)     |
| `FAIL_NON_SERIALIZED`      | Nebenläufiger Zugriff aus mehreren Threads erkannt   |
| `FAIL_CANCELLED`           | Subscriber hat die Subscription gecancelt            |

---

## Beispiele in diesem Modul

| Beispiel | Thema                         | Neue APIs                                           |
|----------|-------------------------------|-----------------------------------------------------|
| 1        | Sinks.One – Grundlagen        | `Sinks.one()`, `tryEmitValue()`, `asMono()`         |
| 2        | Sinks.One – Late-Subscriber   | Wert vor Subscription emittieren, `tryEmitError()`  |
| 3        | Sinks.Many – Unicast          | `unicast().onBackpressureBuffer()`, `asFlux()`      |
| 4        | Sinks.Many – Multicast        | `multicast().directBestEffort()`                    |
| 5        | Sinks.Many – Replay           | `replay().all()`, `replay().limit(n)`               |
| 6        | Thread-sicheres Emittieren    | Retry-Schleife mit `FAIL_NON_SERIALIZED`            |
| 7        | EmitResult auswerten          | Alle EmitResult-Werte und ihre Bedeutung            |

---

## Projektstruktur

```
10_Sinks/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── src/
    └── main/
        └── java/
            └── de/sinks/
                └── Main.java
```

**Starten:**
```bash
./gradlew run
```

---

## Vergleich: 09_Testing → 10_Sinks

| Aspekt         | 09_Testing                             | 10_Sinks                               |
|----------------|----------------------------------------|----------------------------------------|
| Thema          | Pipelines testen mit StepVerifier      | Pipelines von aussen befüllen          |
| Werkzeug       | `StepVerifier`, `TestPublisher`        | `Sinks.One`, `Sinks.Many`             |
| Kontrolle      | Assertion-getriebene Steuerung         | Emitter-seitige Steuerung              |
| Subscriber     | Test-Subscriber                        | beliebige Subscriber                   |

---

## Nächste Schritte

→ **11_Context**: Wie trägt man Metadaten (z.B. Request-IDs) durch die gesamte
  reaktive Pipeline, ohne Parameter durch alle Methoden zu schleifen?
