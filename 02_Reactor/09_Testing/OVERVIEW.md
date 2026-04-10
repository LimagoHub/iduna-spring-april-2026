# 09_Testing – Reaktive Streams testen

## Übersicht

Reaktive Pipelines lassen sich nicht mit gewöhnlichen `assertEquals`-Tests prüfen,
weil Streams asynchron sind und mehrere Signale (`onNext`, `onError`, `onComplete`)
über die Zeit liefern. Reactor stellt zwei Werkzeuge bereit:

| Werkzeug | Zweck |
|---|---|
| `StepVerifier` | Ergebnis einer Pipeline überprüfen |
| `TestPublisher` | Quelle eines Streams programmatisch steuern |

---

## StepVerifier

`StepVerifier` abonniert einen Publisher und prüft die Signale in der Reihenfolge,
in der sie erwartet werden. Der Test läuft blockierend – `verify()` wartet, bis
der Stream abgeschlossen ist.

```java
StepVerifier.create(flux)
    .expectNext(1, 2, 3)
    .expectComplete()
    .verify();
```

**Wichtig: Ohne `.verify()` läuft der Test nicht!**

### Methoden im Überblick

| Methode | Bedeutung |
|---|---|
| `expectNext(T...)` | Prüft Werte in exakter Reihenfolge |
| `expectNextCount(n)` | Prüft, dass genau n weitere Elemente folgen |
| `expectNextMatches(Predicate)` | Prüft Wert mit eigenem Prädikat |
| `assertNext(Consumer)` | Eigene Assertions im Consumer |
| `expectComplete()` | Erwartet `onComplete` |
| `expectError(Class)` | Erwartet `onError` vom angegebenen Typ |
| `expectErrorMessage(String)` | Erwartet `onError` mit dieser Nachricht |
| `verifyComplete()` | Kurzform: `expectComplete().verify()` |
| `verifyError(Class)` | Kurzform: `expectError(Class).verify()` |

---

## Virtuelle Zeit

Zeitbasierte Operatoren (`delayElement`, `interval`, `timeout`) würden Tests
sekundenoder minutenlang blockieren. `withVirtualTime` löst das:

```java
StepVerifier.withVirtualTime(() -> Mono.just("x").delayElement(Duration.ofHours(1)))
    .expectSubscription()
    .thenAwait(Duration.ofHours(1))   // virtuelle Stunde vorspulen
    .expectNext("x")
    .verifyComplete();
```

**Regeln für `withVirtualTime`:**
- Der Publisher muss **innerhalb des Lambdas** erstellt werden
- `thenAwait(Duration)` spult die virtuelle Zeit vor
- `expectNoEvent(Duration)` prüft, dass in dieser Zeit nichts passiert

---

## TestPublisher

`TestPublisher` ist ein Publisher, den man manuell steuert. Haupteinsatzgebiet:
Einen Operator oder eine Service-Methode testen, ohne von der echten Quelle
abhängig zu sein.

```java
TestPublisher<Integer> publisher = TestPublisher.create();

StepVerifier.create(publisher.flux())
    .then(() -> publisher.next(1, 2, 3))
    .expectNext(1, 2, 3)
    .then(publisher::complete)
    .expectComplete()
    .verify();
```

### Methoden

| Methode | Signal |
|---|---|
| `next(T...)` | `onNext` |
| `complete()` | `onComplete` |
| `error(Throwable)` | `onError` |
| `emit(T...) ` | `onNext` + `onComplete` |

### Assertions nach dem Test

```java
publisher.assertWasSubscribed();
publisher.assertWasCancelled();
publisher.assertWasNotCancelled();
```

---

## Typische Fehler

| Fehler | Ursache |
|---|---|
| Test läuft, aber prüft nichts | `verify()` vergessen |
| Test hängt ewig | Publisher sendet kein `onComplete` / `onError` |
| `withVirtualTime` funktioniert nicht | Publisher außerhalb des Lambdas erstellt |
| Test schlägt bei Zeitoperatoren fehl | `thenAwait()` zu kurz gewählt |

---

## Beispiele

| Datei | Inhalt |
|---|---|
| `ReactiveService.java` | Service unter Test (Flux, Mono, Fehler, Verzögerung) |
| `StepVerifierBeispiele.java` | 7 Beispiele: Grundlagen bis virtuelle Zeit |
| `TestPublisherBeispiele.java` | 5 Beispiele: Grundlagen bis Operator-Tests |

### StepVerifier-Beispiele

| # | Thema |
|---|---|
| 1 | Grundlagen: `expectNext`, `expectComplete`, `verify` |
| 2 | `expectNextCount` – Anzahl statt Inhalt prüfen |
| 3 | Fehler erwarten mit `expectError` |
| 4 | Eigene Assertions: `expectNextMatches`, `assertNext` |
| 5 | Mono testen: `verifyComplete`, `verifyError` |
| 6 | Virtuelle Zeit: `withVirtualTime`, `thenAwait`, `expectNoEvent` |
| 7 | `verifyThenAssertThat` – Statistiken nach dem Test |

### TestPublisher-Beispiele

| # | Thema |
|---|---|
| 1 | Grundlagen: `next`, `complete` |
| 2 | Fehler emittieren mit `error` |
| 3 | Einen Operator testen, nicht den Publisher |
| 4 | Schrittweise emittieren mit `.then()` |
| 5 | `assertWasSubscribed`, `assertWasCancelled` |

---

## Projektstruktur

```
09_Testing/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/
│   ├── main/java/de/testing/
│   │   └── ReactiveService.java        ← Code unter Test
│   └── test/java/de/testing/
│       ├── StepVerifierBeispiele.java  ← 7 StepVerifier-Tests
│       └── TestPublisherBeispiele.java ← 5 TestPublisher-Tests
└── OVERVIEW.md
```

---

## Vergleich: 08 vs. 09

| | 08_Backpressure | 09_Testing |
|---|---|---|
| Kernfrage | Wie reguliert man den Fluss? | Wie testet man reaktive Pipelines? |
| Werkzeug | `onBackpressure*`, `BaseSubscriber` | `StepVerifier`, `TestPublisher` |
| Ausführung | `main()` | JUnit 5 Tests (`./gradlew test`) |

---

## Nächste Schritte

Das nächste Modul deckt **Sinks** ab: Wie emittiert man programmatisch Elemente
in einen laufenden Stream? `Sinks.One`, `Sinks.Many`, thread-sichere Emission.
