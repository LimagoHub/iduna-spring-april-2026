# 02_ReactorBasics – Einstieg in Project Reactor

## Übersicht

Dieses Modul führt **Project Reactor** ein – die reaktive Bibliothek, die in Spring WebFlux
und im gesamten Spring-Ökosystem eingesetzt wird.

Es baut direkt auf `01_SimpleFlow` auf: Dieselben Konzepte (Publisher, Subscriber,
Backpressure), aber auf einem deutlich höheren Abstraktionsniveau.

---

## Warum Project Reactor?

Die Java 9 Flow API (`01_SimpleFlow`) zeigt die Mechanik von Reactive Streams –
aber man schreibt viel Boilerplate: `onSubscribe`, `onNext`, Backpressure-Zähler,
Synchronisation per `CountDownLatch`.

Project Reactor implementiert dieselbe Spezifikation und fügt hinzu:

| Java Flow API (01) | Project Reactor (02) |
|---|---|
| `Publisher<T>` selbst implementieren | `Flux<T>` und `Mono<T>` fertig geliefert |
| Backpressure manuell (`request(n)`) | Automatisch, intern verwaltet |
| Transformationen in `onNext` | 100+ Operatoren (`map`, `filter`, `flatMap`, ...) |
| Fehler: try/catch in `onNext` | `onErrorReturn()`, `onErrorContinue()`, `retry()` |
| Synchronisation per `CountDownLatch` | `blockLast()`, `block()` |

---

## Flow.Publisher vs. Flux – gleiche Idee, andere Typen

`Flux` und `java.util.concurrent.Flow.Publisher` sehen strukturell identisch aus –
beide haben genau eine Methode:

```java
// Java 9 – java.util.concurrent.Flow
public interface Flow.Publisher<T> {
    void subscribe(Flow.Subscriber<? super T> subscriber);
}

// Reactive Streams / Reactor – org.reactivestreams
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> subscriber);
}
```

Trotzdem sind sie **nicht kompatibel**:

```java
Flux<Integer> producer = Flux.range(1, 3);

Flow.Publisher<Integer> p = producer;  // COMPILERFEHLER
```

### Warum?

Java prüft **nominale Typenkompatibilität** – nicht strukturelle. Auch wenn zwei
Interfaces dieselben Methodensignaturen haben, sind sie für den Compiler
unterschiedliche Typen, solange kein `implements`-Verhältnis besteht.

`Flux<T>` implementiert `org.reactivestreams.Publisher<T>` – **nicht**
`java.util.concurrent.Flow.Publisher<T>`.

### Historischer Hintergrund

| | Reactive Streams (`org.reactivestreams`) | Java 9 Flow API |
|---|---|---|
| Erschienen | 2015 (als externe Spezifikation) | 2017 (Java 9) |
| Interfaces | `Publisher`, `Subscriber`, `Subscription`, `Processor` | `Flow.Publisher`, `Flow.Subscriber`, `Flow.Subscription`, `Flow.Processor` |
| Methoden | identisch | identisch (1:1 kopiert) |
| Kompatibilität | nein – unterschiedliche Pakete/Typen | nein |

Java 9 hat die Reactive-Streams-Spezifikation **inhaltlich übernommen**, aber als
eigene Interfaces im JDK verpackt. Reactor entstand 2015 und baut auf dem
`org.reactivestreams`-Paket – das konnte nicht nachträglich geändert werden.

### Brücke: JdkFlowAdapter

Reactor liefert einen Adapter, falls man beide Welten verbinden muss:

```java
Flux<Integer> flux = Flux.range(1, 3);

// Flux → Flow.Publisher
Flow.Publisher<Integer> flowPublisher =
    JdkFlowAdapter.publisherToFlowPublisher(flux);

// Flow.Publisher → Flux
Flux<Integer> zurück =
    JdkFlowAdapter.flowPublisherToFlux(flowPublisher);
```

In der Praxis braucht man das selten – Spring WebFlux arbeitet intern durchgehend
mit `org.reactivestreams.Publisher`.

---

## Gradle-Setup

Das Projekt verwendet Gradle mit der Reactor-Abhängigkeit:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.projectreactor:reactor-core:3.7.4")
}
```

Reactor Core enthält `Flux`, `Mono` und alle Kernoperatoren.

---

## Die zwei zentralen Typen

```
reactor.core.publisher
├── Flux<T>   – 0 bis N Elemente  →  entspricht Publisher<T> aus Flow API
└── Mono<T>   – 0 oder 1 Element  →  spezialisiert für einzelne Werte / HTTP-Responses
```

### Flux

```java
Flux<String> flux = Flux.fromIterable(List.of("A", "B", "C"));
flux.subscribe(element -> System.out.println(element));
```

### Mono

```java
Mono<String> mono = Mono.just("Einzelner Wert");
mono.subscribe(wert -> System.out.println(wert));
```

---

## Das Reactive-Streams-Protokoll (zur Erinnerung)

```
Flux/Mono                          Subscriber
   |                                   |
   |  ← subscribe(subscriber)     ←   |
   |                                   |
   |  → onSubscribe(subscription)  →  |
   |                                   |
   |  ← request(n)               ←    |   (Backpressure – intern, nicht sichtbar)
   |                                   |
   |  → onNext(item)             →    |
   |  → onNext(item)             →    |
   |  ...                              |
   |  → onComplete() / onError() →    |
```

In Reactor ist die Backpressure-Steuerung intern – man sieht `request(n)` nicht
im eigenen Code, es läuft automatisch.

---

## Flux ist ein Producer – lazy by design

Ein `Flux` ist kein Container mit fertigen Daten (wie eine `List`), sondern eine
**Beschreibung eines zukünftigen Datenflusses**. Nichts passiert, bevor jemand
`subscribe()` aufruft.

```java
Flux<Integer> producer = Flux.generate(
    () -> 1,
    (zaehler, sink) -> {
        sink.next(zaehler);
        if (zaehler == 3) sink.complete();
        return zaehler + 1;
    }
);
// ↑ Kein einziges Element wurde produziert – nur das Rezept existiert.

producer.subscribe(n -> System.out.println("empfangen: " + n));
// ↑ Erst jetzt läuft der Producer-Code.
```

`Flux.generate()` produziert Elemente **on-demand**, genau so viele wie der
Subscriber anfordert – das ist Backpressure von Anfang an eingebaut.

**Vergleich mit normalen Java-Collections:**

| | `List<T>` | `Flux<T>` |
|---|---|---|
| Daten | sofort im Speicher | erst bei subscribe() produziert |
| Größe | fest, im Voraus bekannt | kann unbegrenzt sein |
| Backpressure | nicht vorhanden | eingebaut |
| Lazy | nein | ja |

---

## Warum ist Flux effizient – besonders bei blockierenden Operationen?

### Das Problem: Thread-per-Request (klassisches Java)

In einer traditionellen Spring-MVC-Anwendung reserviert jede eingehende Anfrage
**einen Thread**, der von Anfang bis Ende blockiert ist:

```
Anfrage 1: Thread-A  [--------DB-Abfrage wartet--------][--------HTTP-Call wartet--------] fertig
Anfrage 2: Thread-B  [--------DB-Abfrage wartet--------][--------HTTP-Call wartet--------] fertig
Anfrage 3: Thread-C  [--------DB-Abfrage wartet--------] ...
...
Anfrage 500: ?       → kein Thread frei → Anfrage wartet oder schlägt fehl
```

Threads sind teuer: ~1 MB Stack pro Thread, Context-Switching durch das OS,
begrenzter Thread-Pool. Bei 500 gleichzeitigen Anfragen mit je 200 ms DB-Wartezeit
sind fast alle Threads die meiste Zeit **idle** – sie warten einfach.

---

### Die Lösung: Event Loop + nicht-blockierende I/O

Reactor setzt (in Kombination mit Netty, dem Standard in Spring WebFlux) auf ein
**Event-Loop-Modell** – dasselbe Prinzip wie Node.js:

```
Event Loop (1 Thread pro CPU-Kern)
│
├── Anfrage 1 kommt rein  → DB-Abfrage starten → Thread freigeben
├── Anfrage 2 kommt rein  → DB-Abfrage starten → Thread freigeben
├── Anfrage 3 kommt rein  → DB-Abfrage starten → Thread freigeben
│
├── [DB antwortet für Anfrage 2] → Thread nimmt Ergebnis entgegen → HTTP-Call starten → freigeben
├── [DB antwortet für Anfrage 1] → Thread nimmt Ergebnis entgegen → HTTP-Call starten → freigeben
│
├── [HTTP-Call für Anfrage 2 fertig] → Antwort senden
├── [HTTP-Call für Anfrage 1 fertig] → Antwort senden
```

**Kernidee:** Der Thread blockiert nie. Er startet eine I/O-Operation, gibt sich
frei, und wenn das Ergebnis da ist, nimmt irgendein freier Thread die Arbeit wieder auf.

Mit 8 CPU-Kernen → 8 Event-Loop-Threads → können tausende gleichzeitige Anfragen
bedient werden, solange die I/O-Operationen nicht-blockierend sind.

---

### Was passiert unter der Haube?

**1. Die Pipeline ist ein Graph von Operatoren**

```java
Flux.fromIterable(liste)   // Source-Operator
    .map(String::length)   // Transform-Operator
    .filter(n -> n > 2)    // Filter-Operator
    .subscribe(...)        // startet den Fluss
```

Beim Aufbau der Kette entsteht intern ein verketteter Graph von Objekten –
jeder Operator wrapp den vorherigen. Noch kein Code läuft.

**2. subscribe() schickt ein Signal rückwärts durch die Kette**

```
subscribe()  →  filter abonniert map  →  map abonniert source  →  source startet
```

Das ist das `onSubscribe`-Signal aus dem Reactive-Streams-Protokoll –
hier automatisch von Reactor verwaltet.

**3. Elemente fließen vorwärts, Backpressure-Signale rückwärts**

```
Source → map → filter → Subscriber
                ←  request(n)  ←      (Backpressure)
```

Der Subscriber fordert nur so viele Elemente an, wie er verarbeiten kann.
Die Source produziert nicht mehr als angefordert – kein Overflow, kein
`OutOfMemoryError` bei großen Datenmengen.

**4. Scheduler bestimmen auf welchem Thread was läuft**

Ohne `subscribeOn`/`publishOn` läuft alles synchron im aufrufenden Thread.
In echten Anwendungen schaltet man Scheduler ein:

```java
Flux.fromIterable(liste)
    .subscribeOn(Schedulers.boundedElastic())  // I/O-Thread: hier läuft die Source
    .map(this::transformiere)
    .publishOn(Schedulers.parallel())          // ab hier: CPU-Thread für Transformation
    .subscribe(...)
```

| Scheduler | Thread-Pool | Verwendung |
|---|---|---|
| `Schedulers.parallel()` | N Threads (N = CPU-Kerne) | CPU-intensive Operationen |
| `Schedulers.boundedElastic()` | Elastischer Pool (max. ~10×CPU) | Blocking I/O (DB, Datei, HTTP) |
| `Schedulers.single()` | 1 Thread | Serialisierung, einfache Tasks |

**5. Nicht-blockierende I/O (das eigentliche Geheimnis)**

Der Effizienzgewinn entsteht nicht durch Reactor allein, sondern durch
nicht-blockierende I/O auf Betriebssystemebene (Java NIO / Netty):

- Eine DB-Abfrage registriert einen **Callback** beim OS
- Das OS benachrichtigt Java, wenn Daten bereit sind (kein Thread wartet)
- Reactor verbindet diesen OS-Callback mit dem nächsten Schritt in der Pipeline

```
Thread startet DB-Query
    └─ OS-NIO-Selektor übernimmt die Wartearbeit
           └─ Daten bereit → OS weckt Reactor
                  └─ Reactor ruft onNext() auf → Pipeline läuft weiter
```

Der Thread ist die gesamte Wartezeit **frei** für andere Anfragen.

---

## Beispiele in `Main.java`

### Beispiel 0 – Flux als Producer (lazy)

```java
Flux<Integer> producer = Flux.generate(
    () -> 1,
    (zaehler, sink) -> {
        System.out.println("[Producer] produziere Element " + zaehler);
        sink.next(zaehler);
        if (zaehler == 3) sink.complete();
        return zaehler + 1;
    }
);

System.out.println("Flux definiert – noch kein Element produziert");
producer.subscribe(n -> System.out.println("[Subscriber] empfangen: " + n));
```

Ausgabe:
```
Flux definiert – noch kein Element produziert
[Producer] produziere Element 1
[Subscriber] empfangen: 1
[Producer] produziere Element 2
[Subscriber] empfangen: 2
[Producer] produziere Element 3
[Subscriber] empfangen: 3
```

Jedes Element wird erst produziert, wenn der Subscriber es anfordert (Backpressure).

---

### Beispiel 1 – Flux aus Liste

```java
Flux.fromIterable(List.of("Hallo", "Reactor", "Welt"))
    .subscribe(element -> System.out.println("Empfangen: " + element));
```

`subscribe()` startet den Datenfluss. Ohne `subscribe()` passiert nichts –
`Flux` ist **lazy** (wie `SubmissionPublisher` erst nach `subscribe()`).

---

### Beispiel 2 – Operatoren: `map()` und `filter()`

```java
Flux.range(1, 10)
    .filter(n -> n % 2 == 0)   // 2, 4, 6, 8, 10
    .map(n -> n * n)            // 4, 16, 36, 64, 100
    .subscribe(
        n   -> System.out.println("Wert: " + n),
        err -> System.err.println("Fehler: " + err),
        ()  -> System.out.println("Stream abgeschlossen")
    );
```

`subscribe()` kann bis zu drei Lambdas erhalten: `onNext`, `onError`, `onComplete`.

---

### Beispiel 3 – Mono

```java
Mono.just("Ich bin ein einziger Wert")
    .subscribe(wert -> System.out.println(wert));

Mono.empty()   // kein Wert, aber sauberes onComplete
    .subscribe(
        wert -> {},
        err  -> {},
        ()   -> System.out.println("Leere Mono abgeschlossen")
    );
```

---

### Beispiel 4 – Dieselbe Pipeline wie in 01_SimpleFlow

**01_SimpleFlow** brauchte drei Klassen + `CountDownLatch`:

```
SubmissionPublisher<String> → MyProcessor (String::length) → EndSubscriber<Integer>
```

**Mit Reactor** – eine Methodenkette:

```java
Flux.fromIterable(worte)
    .map(String::length)
    .doOnNext(n -> System.out.println("Empfangen: " + n))
    .doOnComplete(() -> System.out.println("Stream vollständig verarbeitet."))
    .blockLast();
```

`blockLast()` blockiert den Hauptthread bis zum Ende des Streams –
der Ersatz für `CountDownLatch` aus `EndSubscriber`.

> **Hinweis für die Praxis:** In produktivem reaktivem Code (z.B. Spring WebFlux)
> vermeidet man `block*()`. Hier dient es nur dem didaktischen Vergleich.

---

### Beispiel 5 – Fehlerbehandlung

**`onErrorReturn()`** – Fallback-Wert, Stream endet danach:

```java
Flux.just("10", "zwanzig", "30")
    .map(Integer::parseInt)          // "zwanzig" → NumberFormatException
    .onErrorReturn(-1)               // Fallback: -1, Stream endet
    .subscribe(n -> System.out.println(n));
// Ausgabe: 10, -1
```

**`onErrorContinue()`** – fehlerhafte Elemente überspringen, Stream läuft weiter:

```java
Flux.just("10", "zwanzig", "30")
    .map(Integer::parseInt)
    .onErrorContinue((err, obj) -> System.out.println("Übersprungen: " + obj))
    .subscribe(n -> System.out.println(n));
// Ausgabe: 10, [Übersprungen: zwanzig], 30
```

In `01_SimpleFlow` war das die "skip and continue"-Strategie im `try/catch` von `onNext`.

---

## Erwartete Ausgabe

```
=== Reactor Basics ===

--- Beispiel 1: Flux aus Liste ---
  Empfangen: Hallo
  Empfangen: Reactor
  Empfangen: Welt

--- Beispiel 2: map() und filter() ---
  Wert: 4
  Wert: 16
  Wert: 36
  Wert: 64
  Wert: 100
  Stream abgeschlossen

--- Beispiel 3: Mono ---
  Mono-Wert: Ich bin ein einziger Wert
  Leere Mono abgeschlossen (kein Wert)

--- Beispiel 4: Pipeline wie 01_SimpleFlow ---
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

--- Beispiel 5: Fehlerbehandlung ---
  Wert: 10
  Wert: -1

  Übersprungen 'zwanzig': For input string: "zwanzig"
  Wert: 10
  Wert: 30
```

---

## Projektstruktur

```
02_ReactorBasics/
├── build.gradle.kts          ← Gradle + reactor-core Abhängigkeit
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/reactorbasics/
│   └── Main.java             ← Alle 5 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 01 vs 02

| Aufgabe | 01_SimpleFlow | 02_ReactorBasics |
|---|---|---|
| Publisher erzeugen | `new SubmissionPublisher<>()` | `Flux.fromIterable(...)` |
| Transformieren | Eigene `MyProcessor`-Klasse | `.map(String::length)` |
| Subscriben | Eigene `EndSubscriber`-Klasse | `.subscribe(n -> ...)` |
| Synchronisieren | `CountDownLatch` | `.blockLast()` |
| Fehler überspringen | `try/catch` in `onNext` | `.onErrorContinue(...)` |
| Fehler-Fallback | `closeExceptionally()` | `.onErrorReturn(-1)` |

---

## Nächste Schritte

Das nächste Modul geht tiefer in Operators, Scheduling und asynchrone Verarbeitung.
