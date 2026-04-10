# 01_SimpleFlow – Java 9 Flow API (Reactive Streams)

## Übersicht

Dieses Projekt demonstriert die grundlegenden Konzepte der **Reactive Streams**-Spezifikation
anhand der Java 9 Flow API (`java.util.concurrent.Flow`).

Es ist der Einstieg in den Kurs und dient als Vergleichsbasis für Project Reactor (Modul 02).

---

## Was sind Reactive Streams?

Reactive Streams ist eine Spezifikation (https://www.reactive-streams.org) für
**asynchrone Datenströme mit nicht-blockierendem Backpressure**.

| Begriff | Bedeutung |
|---|---|
| **Asynchron** | Produzent und Konsument laufen in verschiedenen Threads |
| **Backpressure** | Der Konsument steuert, wie viele Elemente er bereit ist zu empfangen |
| **Non-blocking** | Kein Thread wird durch Warten auf Daten blockiert |

Java 9 hat diese Spezifikation unter `java.util.concurrent.Flow` in den JDK integriert –
als **gemeinsame Schnittstelle** für verschiedene Implementierungen
(Reactor, RxJava, Akka Streams etc.).

---

## Kerninterfaces der Java Flow API

```
java.util.concurrent.Flow
├── Publisher<T>      – Datenquelle: produziert Elemente vom Typ T
├── Subscriber<T>     – Datenempfänger: konsumiert Elemente vom Typ T
├── Processor<T, R>   – Transformation: ist gleichzeitig Subscriber<T> und Publisher<R>
└── Subscription      – Verbindung zwischen Publisher und Subscriber (Backpressure-Steuerung)
```

### Das Reactive-Streams-Protokoll

```
Publisher                    Subscriber
   |                             |
   |  ← subscribe(subscriber) ← |
   |                             |
   |  → onSubscribe(sub)    →   |
   |                             |
   |  ← request(n)          ← |   (Backpressure: "ich bin bereit für n Elemente")
   |                             |
   |  → onNext(item1)       →   |
   |  → onNext(item2)       →   |
   |  ...                        |
   |  → onComplete()        →   |   (oder onError() bei Fehler)
```

**Wichtige Regel:** Ohne `request(n)` sendet der Publisher nichts. Das ist der
Kernmechanismus für Backpressure.

---

## Projektstruktur

```
01_SimpleFlow/
├── src/
│   ├── module-info.java
│   └── de/
│       ├── application/
│       │   └── Main.java          ← Einstiegspunkt, Pipeline-Aufbau
│       ├── processors/
│       │   └── MyProcessor.java   ← Transformer: String → Integer
│       └── subscriber/
│           └── EndSubscriber.java ← Endverbraucher mit Completion-Synchronisation
└── OVERVIEW.md
```

---

## Die Verarbeitungspipeline

```
SubmissionPublisher<String>
        │
        │  liefert: "1", "2", "drei", "4", "fünf", ...
        ▼
MyProcessor  (String → Integer)
        │
        │  Transformation: String::length
        │  "1"→1, "2"→1, "drei"→4, "4"→1, "fünf"→4
        ▼
EndSubscriber<Integer>
        │
        │  Ausgabe auf der Konsole
        ▼
```

---

## Klassen im Detail

### `EndSubscriber<T>`

Der einfachste mögliche Subscriber: Gibt jedes empfangene Element aus.

```java
public class EndSubscriber<T> implements Subscriber<T>
```

**Wichtige Design-Entscheidung:** Der `CountDownLatch completionLatch`

```java
private final CountDownLatch completionLatch = new CountDownLatch(1);
```

Der Latch wird in `onComplete()` auf 0 gesetzt. Die Methode `awaitCompletion()`
blockiert den Main-Thread, bis der Stream vollständig abgeschlossen ist.

**Warum brauchen wir das?**

`SubmissionPublisher` verarbeitet Elemente asynchron im `ForkJoinPool.commonPool()`.
Ohne Synchronisation würde der Main-Thread das Programm beenden, bevor alle
Elemente verarbeitet wurden.

| Ansatz | Problem |
|---|---|
| `Thread.sleep(1000)` | Willkürlich, fehleranfällig |
| `executor.awaitTermination()` | `ForkJoinPool.commonPool()` kann nicht heruntergefahren werden |
| `CountDownLatch` | Korrekt, deterministisch, idiomatisch |

---

### `MyProcessor`

Ein Processor transformiert einen Datentyp in einen anderen.

```java
public class MyProcessor extends SubmissionPublisher<Integer>
        implements Processor<String, Integer>
```

**Implementierungsstrategie:** Statt `Processor` vollständig selbst zu implementieren,
erbt `MyProcessor` von `SubmissionPublisher<Integer>`. Damit übernimmt die Superklasse:
- Verwaltung aller downstream Subscriber
- Thread-sicheres Senden
- Backpressure-Puffer
- `close()` und `closeExceptionally()` für Terminierung

Wir implementieren nur die **Subscriber-Seite** (upstream-facing):
`onSubscribe`, `onNext`, `onError`, `onComplete`.

**Fehlerbehandlung in `onNext`:**

```java
try {
    int result = function.apply(item);
    submit(result);
} catch (Exception e) {
    System.err.println("Fehler bei Transformation: " + e.getMessage());
}
subscription.request(1); // auch bei Fehler: nächstes Element anfordern
```

Strategie: **"skip and continue"** – fehlerhafte Elemente werden übersprungen,
die Verarbeitung geht weiter. Alternative wäre `closeExceptionally(e)` für
"fail fast"-Verhalten.

---

### `Main.java` – Pipeline-Aufbau

Die Pipeline wird **von hinten nach vorne** aufgebaut:

```java
// 1. Endverbraucher erstellen
EndSubscriber<Integer> endSubscriber = new EndSubscriber<>();

// 2. Processor erstellen und mit Endverbraucher verbinden
MyProcessor processor = new MyProcessor(String::length);
processor.subscribe(endSubscriber);  // processor als Publisher

// 3. Publisher erstellen und mit Processor verbinden
SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
publisher.subscribe(processor);  // publisher → processor

// 4. Daten senden
worte.forEach(publisher::submit);

// 5. Stream schließen
publisher.close();

// 6. Warten bis alles verarbeitet ist
endSubscriber.awaitCompletion();
```

**Warum von hinten nach vorne?**

`subscribe()` gibt bei `SubmissionPublisher` sofort zurück. Würden wir
`publisher.subscribe(processor)` aufrufen und dann erst `processor.subscribe(endSubscriber)`,
könnten bereits Elemente durch den Processor fließen, bevor der EndSubscriber bereit ist.

---

## Ausgabe des Programms

```
Empfangen: 1
Empfangen: 1
Empfangen: 4
Empfangen: 1
Empfangen: 4
Empfangen: 1
Empfangen: 1
Empfangen: 4
Empfangen: 1
Empfangen: 4
Stream vollständig verarbeitet.
Alle Elemente verarbeitet. Programm beendet.
```

*(Die Reihenfolge ist deterministisch, da Backpressure mit `request(1)` eine
sequentielle Verarbeitung erzwingt.)*

---

## Bezug zu Project Reactor

Die Java Flow API ist **niedriglevelig** – man implementiert viel selbst
(`onSubscribe`, `onNext`, Backpressure-Logik, Thread-Synchronisation).

Project Reactor (Modul 02) baut auf denselben Prinzipien auf, bietet aber:

| Java Flow API | Project Reactor |
|---|---|
| `Publisher<T>` | `Flux<T>` (0..N Elemente), `Mono<T>` (0..1 Element) |
| Manuelle Backpressure | Automatisch, konfigurierbar |
| Kein Operator-Ökosystem | 100+ Operatoren (map, filter, flatMap, zip, ...) |
| Synchronisation per Hand | Eingebaut (z.B. `blockLast()`) |
| Kein Fehler-Retry | `retry()`, `onErrorReturn()`, etc. |

Das nächste Modul zeigt, wie Project Reactor dieselbe Pipeline eleganter ausdrückt.
