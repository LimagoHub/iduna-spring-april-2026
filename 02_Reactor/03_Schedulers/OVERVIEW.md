# 03_Schedulers – Threading in Project Reactor

## Übersicht

Dieses Modul erklärt, wie Reactor mit Threads umgeht – und räumt dabei mit dem
häufigsten Irrtum auf:

> **Reactor ist NICHT automatisch asynchron.**

Ohne explizite Konfiguration läuft alles synchron im aufrufenden Thread –
genau wie normaler Java-Code. Schedulers sind das Werkzeug, um das zu ändern.

---

## Der Kern-Irrtum: "Reactive = automatisch async"

```java
Flux.range(1, 3)
    .map(n -> n * 2)
    .subscribe(n -> System.out.println(n));

// Läuft alles auf dem main-Thread – synchron, nacheinander.
// Kein neuer Thread, keine Magie.
```

Reactor trennt zwei Dinge bewusst voneinander:
- **Was** passiert (Pipeline-Definition)
- **Wo** es passiert (Scheduler-Konfiguration)

---

## Die drei wichtigsten Schedulers

| Scheduler | Thread-Pool | Verwendung |
|---|---|---|
| `Schedulers.boundedElastic()` | Elastisch (max. ~10×CPU) | Blocking I/O: DB, HTTP, Datei |
| `Schedulers.parallel()` | N Threads (N = CPU-Kerne) | CPU-intensive Berechnungen |
| `Schedulers.single()` | 1 Thread | Serialisierung, einfache Tasks |

**Faustregel:**
- I/O-Operationen → `boundedElastic()`
- CPU-Arbeit → `parallel()`

---

## subscribeOn vs. publishOn

Das sind die zwei Operatoren, um Threads in der Pipeline zu steuern.
Sie klingen ähnlich, wirken aber grundlegend anders.

### subscribeOn – wirkt rückwärts (auf die Source)

```
Flux.range(1, 3)          ← läuft auf boundedElastic
    .map(...)              ← läuft auf boundedElastic
    .subscribeOn(Schedulers.boundedElastic())
    .subscribe(...)        ← läuft auf boundedElastic
```

- Bestimmt, auf welchem Thread die **Subscription startet** (= wo die Source produziert)
- Wirkt **rückwärts** durch die Pipeline – egal wo es steht, beeinflusst immer die Source
- Alle Operatoren vor einem `publishOn()` laufen auf diesem Thread

### publishOn – wirkt vorwärts (ab dieser Stelle)

```
Flux.range(1, 3)          ← läuft auf main
    .map(...)              ← läuft auf main
    .publishOn(Schedulers.parallel())
    .map(...)              ← läuft auf parallel
    .subscribe(...)        ← läuft auf parallel
```

- Wechselt den Thread **vorwärts ab der Stelle**, wo es steht
- Alles davor: bisheriger Thread. Alles danach: neuer Scheduler.
- Kann mehrfach in einer Pipeline verwendet werden

---

## Kombination: I/O lesen + CPU verarbeiten

Das typische Produktionsmuster:

```java
Flux.fromIterable(ids)
    .subscribeOn(Schedulers.boundedElastic())   // DB-Abfrage auf I/O-Thread
    .map(id -> ladeDatensatzAusDB(id))          // läuft auf boundedElastic
    .publishOn(Schedulers.parallel())           // ab hier: CPU-Thread
    .map(datensatz -> verarbeite(datensatz))    // läuft auf parallel
    .subscribe(result -> speichere(result));
```

```
I/O-Thread (boundedElastic)    CPU-Thread (parallel)
│                              │
│  Source produziert IDs       │
│  DB-Abfrage läuft            │
│                              │
│──── publishOn ──────────────►│
│                              │  Transformation
│                              │  Ergebnis
```

---

## Parallele Ausführung mit flatMap

`flatMap()` startet pro Element einen eigenen Sub-Publisher – wenn jeder auf
einem eigenen Thread läuft, entstehen echte parallele Ausführungen:

```java
Flux.just("A", "B", "C")
    .flatMap(item ->
        Mono.fromCallable(() -> verarbeite(item))
            .subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
```

```
main-Thread:
  flatMap startet Sub-Mono für A  →  boundedElastic-1: verarbeite(A)
  flatMap startet Sub-Mono für B  →  boundedElastic-2: verarbeite(B)
  flatMap startet Sub-Mono für C  →  boundedElastic-3: verarbeite(C)
                                     ↓ alle drei gleichzeitig
```

Das ist das Grundmuster für parallele HTTP-Calls oder DB-Abfragen.

---

## Beispiele in `Main.java`

### Beispiel 0 – Ohne Scheduler (synchron)

```java
Flux.range(1, 3)
    .map(n -> { /* Thread: main */ return n * 2; })
    .subscribe(n -> { /* Thread: main */ });
```

Ausgabe: alle Zeilen zeigen `main`.

---

### Beispiel 1 – subscribeOn

```java
Flux.range(1, 3)
    .map(n -> { /* Thread: boundedElastic-1 */ return n * 2; })
    .subscribeOn(Schedulers.boundedElastic())
    .blockLast();
```

Ausgabe: alle Zeilen zeigen `boundedElastic-1` (oder ähnlich).

---

### Beispiel 2 – publishOn

```java
Flux.range(1, 3)
    .map(n -> { /* Thread: main */ return n; })
    .publishOn(Schedulers.parallel())
    .map(n -> { /* Thread: parallel-1 */ return n * 2; })
    .blockLast();
```

Ausgabe: erste map `main`, zweite map `parallel-1`.

---

### Beispiel 3 – subscribeOn + publishOn kombiniert

```java
Flux.range(1, 3)
    .subscribeOn(Schedulers.boundedElastic())
    .map(n -> { /* Thread: boundedElastic-1 */ return n; })
    .publishOn(Schedulers.parallel())
    .map(n -> { /* Thread: parallel-1 */ return n * 2; })
    .blockLast();
```

---

### Beispiel 4 – Parallele Mono-Aufgaben

```java
Flux.just("Aufgabe-A", "Aufgabe-B", "Aufgabe-C")
    .flatMap(aufgabe ->
        Mono.fromCallable(() -> {
            Thread.sleep(100); // simulierte I/O
            return aufgabe + " erledigt";
        }).subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
```

Ausgabe: alle drei Aufgaben starten fast gleichzeitig auf verschiedenen Threads.

---

## Projektstruktur

```
03_Schedulers/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/schedulers/
│   └── Main.java             ← Alle 5 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 02 vs. 03

| | 02_ReactorBasics | 03_Schedulers |
|---|---|---|
| Threading | immer main-Thread | konfigurierbar via Scheduler |
| I/O-Operationen | blockieren den Thread | auf boundedElastic auslagern |
| CPU-Arbeit | auf main | auf parallel auslagern |
| Parallelismus | nein | ja, via flatMap + subscribeOn |

---

## Thread-Wechsel innerhalb der Pipeline

Nach einem `publishOn` kann man **erneut** mit `publishOn` den Thread wechseln —
`subscribeOn` funktioniert dafür **nicht** (wirkt nur rückwärts auf die Source, und
nur der erste `subscribeOn` zählt).

```java
Flux.fromIterable(data)
    .subscribeOn(Schedulers.boundedElastic())  // blockierendes Lesen der Quelle
    .map(this::cpuIntensiveWork)
    .publishOn(Schedulers.parallel())          // CPU-Arbeit auf parallel-Pool
    .map(this::transform)
    .publishOn(Schedulers.boundedElastic())    // zurück auf elastic für blockierendes Schreiben
    .subscribe(this::blockingWrite);
```

**Faustregel für mehrere Wechsel:**

| Zweck | Operator |
|---|---|
| Blockierende Quelle (I/O, DB-Reads) | `subscribeOn(boundedElastic())` |
| Thread-Wechsel innerhalb der Chain | `publishOn(...)` |
| Blockierendes Schreiben am Ende | nochmal `publishOn(boundedElastic())` |

---

## Scheduler-Strategien im mehrschichtigen System

### Weitere Scheduler-Optionen

| Scheduler | Verwendung |
|---|---|
| `boundedElastic()` | Blocking I/O (Standard für DB, Dateien, externe Calls) |
| `parallel()` | CPU-intensive Berechnungen |
| `single()` | Sequentielle Koordination, ein Thread |
| `immediate()` | Kein Hop – bleibt auf aktuellem Thread |
| `fromExecutorService(...)` | Eigener Executor (z.B. für Prioritäten) |

**Java 21+:** Virtual Threads als Alternative zu `boundedElastic`:
```java
Schedulers.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor())
```

### Typische Pipeline: MongoDB → Business Layer → Backend

```
[MongoDB Reactive Driver]
        ↓  (Netty Event Loop — non-blocking, kein subscribeOn nötig)
[Repository Layer]
        ↓  publishOn(boundedElastic)  ← nur wenn blocking (z.B. sync Treiber, JPA)
[Business Layer]
        ↓  publishOn(parallel)        ← CPU-intensive Transformationen
[Controller / WebFlux]
        ↓  (Reactor Netty übernimmt)
[HTTP Response]
```

> **Wichtig:** Der reaktive MongoDB-Treiber (Spring Data Reactive) ist bereits
> non-blocking und nutzt seinen eigenen Netty-Scheduler. `subscribeOn` ist dort
> nur nötig, wenn blocking Code eingebaut wird (sync Treiber, Legacy-Code).

### Beispiel: Service-Schicht mit Layer-spezifischen Schedulern

```java
// Repository (reaktiv — kein expliziter Scheduler nötig)
Flux<Order> orders = orderRepository.findByCustomerId(id);

// Service: CPU-Arbeit auf parallel
orders
    .publishOn(Schedulers.parallel())
    .map(this::enrichWithBusinessRules)
    .map(this::calculatePricing)

    // Falls irgendwo blocking (z.B. externer Call mit Legacy-Client)
    .publishOn(Schedulers.boundedElastic())
    .flatMap(o -> Mono.fromCallable(() -> legacyClient.fetch(o)))
    .subscribe(...);
```

**Wo konfiguriert man das?**
- In der **Service-Schicht** (nicht im Controller, nicht im Repository)
- Nur dort eingreifen, wo blocking Code auftaucht oder CPU-Arbeit anfällt
- Spring WebFlux und reaktive Treiber regeln den Rest automatisch

---

## Nächste Schritte

Das nächste Modul geht tiefer in `flatMap` und asynchrone Komposition –
der wichtigste Operator für echte reaktive Anwendungen.
