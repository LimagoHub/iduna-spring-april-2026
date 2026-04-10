# Reaktive Programmierung mit Project Reactor

Ein praxisorientierter Kurs, der von den Grundlagen der Java 9 Flow API bis zu
den fortgeschrittenen Konzepten von Project Reactor führt.

---

## Hintergrund: Warum reaktive Programmierung?

Klassische Java-Anwendungen arbeiten nach dem **Thread-per-Request-Prinzip**: Jede
eingehende Anfrage belegt einen Thread – von der ersten Zeile bis zur letzten Antwort.
Während dieser Thread auf eine Datenbankabfrage oder einen HTTP-Call wartet, ist er
blockiert und für nichts anderes nutzbar. Bei hoher Last und langen I/O-Zeiten führt
das schnell zu erschöpften Thread-Pools.

Reaktive Programmierung löst dieses Problem mit einem anderen Ansatz: Ein Thread
startet eine I/O-Operation und gibt sich sofort wieder frei. Wenn die Daten
bereitstehen, nimmt irgendein freier Thread die Arbeit wieder auf. Dieses
**Event-Loop-Modell** (dasselbe Prinzip wie Node.js oder Nginx) erlaubt es, mit
wenigen Threads tausende gleichzeitige Anfragen zu bedienen.

Der Schlüssel dazu ist das **Reactive Streams-Protokoll**: eine Spezifikation für
asynchrone Datenströme mit nicht-blockierendem **Backpressure**. Backpressure bedeutet,
dass der Konsument steuert, wie viele Elemente er bereit ist zu empfangen – der Produzent
sendet nicht einfach alles auf einmal.

---

## Die Verwandschaft zur Java 9 Flow API

Die Reactive Streams-Spezifikation erschien 2015 als externe Bibliothek
(`org.reactivestreams`) mit vier Interfaces: `Publisher`, `Subscriber`, `Subscription`
und `Processor`. Project Reactor entstand in derselben Zeit und implementiert diese
Spezifikation.

Mit Java 9 (2017) übernahm das JDK diese Interfaces unter `java.util.concurrent.Flow` –
inhaltlich identisch, aber als eigene Typen im JDK verpackt. Reactor konnte seine
Implementierung daraufhin nicht einfach umstellen, da das die Kompatibilität gebrochen
hätte. Die Folge: `Flux` und `Flow.Publisher` sehen strukturell gleich aus, sind aber
für den Java-Compiler unterschiedliche Typen. Für den seltenen Fall, dass beide Welten
verbunden werden müssen, bietet Reactor den `JdkFlowAdapter`.

Das erste Kursmodul nutzt die Java 9 Flow API bewusst als Einstieg: Man sieht das
Protokoll in seiner rohen Form – mit eigenem `Subscriber`, manuellem `request(n)` und
`CountDownLatch` für die Synchronisation. Das schafft das Fundament, um in Modul 02 zu
verstehen, was Project Reactor einem abnimmt.

---

## Kursstruktur

Der Kurs ist in 13 Module aufgeteilt. Modul 01 verwendet reines Java (kein Build-Tool),
ab Modul 02 wird Gradle mit der Reactor-Abhängigkeit eingesetzt. Jedes Modul ist ein
eigenständiges Gradle-Projekt (oder IntelliJ-Modul) und enthält eine `OVERVIEW.md` mit
ausführlicher Dokumentation zu den behandelten Konzepten.

---

## Module

### 01 – SimpleFlow *(Java 9 Flow API)*

Einstieg ohne Reactor-Bibliothek: Eine Verarbeitungspipeline mit der Java 9 Flow API
(`java.util.concurrent.Flow`). Ein `SubmissionPublisher` liefert Strings, ein
`MyProcessor` transformiert sie per `String::length` zu Integers, ein `EndSubscriber`
gibt sie aus. Das Projekt zeigt das Reactive-Streams-Protokoll in seiner rohen Form
und dient als Vergleichsbasis für alle folgenden Module.

**Kernthemen:** Publisher/Subscriber/Processor/Subscription, manuelles `request(n)`,
`CountDownLatch` als Synchronisationsmittel, "skip and continue"-Fehlerbehandlung.

---

### 02 – ReactorBasics *(Flux, Mono, Grundoperatoren)*

Einführung in Project Reactor: dieselbe Pipeline wie in Modul 01, jetzt als einzeilige
Operator-Kette. Das Modul erklärt den Unterschied zwischen `Flux<T>` (0..N Elemente)
und `Mono<T>` (0..1 Element), warum `Flux` lazy ist und erst bei `subscribe()` produziert,
und wie Reactor intern mit Backpressure und Threading umgeht. Außerdem: warum `Flux` und
`Flow.Publisher` trotz identischer Methodensignaturen nicht kompatibel sind.

**Kernthemen:** `Flux`, `Mono`, `map()`, `filter()`, `subscribe()`, `blockLast()`,
`onErrorReturn()`, `onErrorContinue()`, Lazy-Evaluation, Event-Loop-Modell.

---

### 03 – Schedulers *(Threading)*

Reactor ist **nicht** automatisch asynchron – ohne Konfiguration läuft alles synchron
im aufrufenden Thread. Dieses Modul räumt mit diesem häufigen Irrtum auf und zeigt,
wie `subscribeOn()` und `publishOn()` den Thread-Wechsel steuern. Kernunterschied:
`subscribeOn` wirkt rückwärts auf die Quelle, `publishOn` wechselt den Thread ab
dieser Stelle vorwärts in der Kette.

**Kernthemen:** `Schedulers.boundedElastic()`, `Schedulers.parallel()`,
`Schedulers.single()`, `subscribeOn()` vs. `publishOn()`, Parallelismus via
`flatMap + subscribeOn`.

---

### 04 – FlatMap *(Asynchrone Komposition)*

`flatMap` ist der wichtigste Operator in reaktiver Programmierung: Er nimmt jeden Wert
und expandiert ihn zu einem eigenen Publisher (`T → Publisher<R>`). Da alle
Sub-Publisher sofort abonniert werden, entsteht echter Parallelismus – ideal für
parallele HTTP-Calls oder Datenbankabfragen. Das Modul vergleicht auch `concatMap`
(reihenfolgeerhaltend, sequenziell) und `switchMap` (nur das letzte Element zählt,
z.B. für Typeahead-Suche).

**Kernthemen:** `flatMap()`, `concatMap()`, `switchMap()`, `flatMapIterable()`,
`Mono.fromCallable()`, Parallelismus-Muster.

---

### 05 – ErrorHandling *(Fehlerbehandlung)*

In Reactive Streams beendet ein Fehler den Stream – danach kommt nichts mehr. Dieses
Modul zeigt alle Strategien, um mit Fehlern umzugehen: statische Fallback-Werte,
dynamische Fallback-Publisher, Fehlertransformation für Schichtenarchitektur,
automatisches Wiederholen und exponentiellen Backoff mit Jitter. Ein besonderer Fokus
liegt auf der Fehler-Isolation in `flatMap`-Sub-Publishern.

**Kernthemen:** `onErrorReturn()`, `onErrorResume()`, `onErrorMap()`, `doOnError()`,
`retry()`, `retryWhen(Retry.backoff(...))`, Fehler-Isolation in `flatMap`.

---

### 06 – HotColdPublishers *(Hot vs. Cold)*

Cold Publisher starten ihre Produktion neu für jeden Subscriber (wie eine DVD).
Hot Publisher produzieren unabhängig davon, wer gerade zuschaut (wie ein Radiosender).
Das Modul zeigt, wie man Cold Publisher in Hot umwandelt (`publish()`, `autoConnect()`,
`share()`) und wie `cache()` vergangene Elemente für späte Subscriber vorhält.
Außerdem: `Sinks` als programmatische Brücke zwischen imperativem Code und reaktiven Streams.

**Kernthemen:** Cold vs. Hot, `ConnectableFlux`, `publish()`, `connect()`,
`autoConnect()`, `share()`, `cache()`, `Sinks`.

---

### 07 – StreamCombination *(Streams zusammenführen)*

Reaktive Anwendungen arbeiten selten mit nur einem Stream. Dieses Modul zeigt vier
Operatoren zum Kombinieren mehrerer Publisher: `concat()` (sequenziell, Reihenfolge
garantiert), `merge()` (parallel, nach Ankunftszeit), `zip()` (paarweise, gleicher
Index) und `combineLatest()` (immer das neueste Paar bei jeder neuen Emission).

**Kernthemen:** `concat()`, `merge()`, `zip()`, `combineLatest()`,
`concatWith()`, `mergeWith()`, `zipWith()`.

---

### 08 – Backpressure *(Fluss regulieren)*

Was passiert, wenn der Publisher schneller produziert als der Subscriber verarbeiten
kann? Das Modul erklärt alle Backpressure-Strategien: manuelles `request(n)` mit
`BaseSubscriber`, Prefetch-Batches mit `limitRate()`, Puffern, Verwerfen und
Behalten-des-letzten-Elements. Der Unterschied zwischen den Strategien liegt im
Umgang mit Datenverlust und Speicherverbrauch.

**Kernthemen:** `BaseSubscriber`, `limitRate()`, `onBackpressureBuffer()`,
`onBackpressureDrop()`, `onBackpressureLatest()`.

---

### 09 – Testing *(Reaktive Streams testen)*

Reaktive Pipelines lassen sich nicht mit normalem `assertEquals` testen, weil Streams
asynchron sind und multiple Signale über die Zeit liefern. `StepVerifier` abonniert
einen Publisher und prüft die Signale in erwarteter Reihenfolge. `TestPublisher`
erlaubt es, Signale manuell zu steuern – nützlich für das Testen einzelner Operatoren.
Für zeitbasierte Operatoren (`delayElement`, `interval`) gibt es virtuelle Zeit.

**Kernthemen:** `StepVerifier`, `TestPublisher`, `withVirtualTime()`,
`thenAwait()`, `expectNoEvent()`, JUnit 5.

---

### 10 – Sinks *(Programmatisch emittieren)*

Ein Sink ist ein programmatischer Publisher: Er ermöglicht es, aus imperativem Code
(Callbacks, Event-Listener, andere Threads) Elemente in eine reaktive Pipeline
einzuspeisen. Das Modul zeigt `Sinks.One` für einzelne Werte, `Sinks.Many` mit den
Strategien Unicast, Multicast und Replay sowie thread-sicheres Emittieren mit
`tryEmit*()` und die Bedeutung der verschiedenen `EmitResult`-Werte.

**Kernthemen:** `Sinks.One`, `Sinks.Many`, Unicast/Multicast/Replay,
`tryEmitNext()`, `EmitResult`, thread-sichere Emission.

---

### 11 – Context *(Querschnittsdaten transportieren)*

`ThreadLocal` funktioniert in reaktiven Systemen nicht, weil ein Request ständig
zwischen Threads wechselt. Der Reactor `Context` ist der reaktive Ersatz: ein
immutabler Key-Value-Store, der **entgegen dem Datenstrom** fließt (von Subscriber
zur Quelle). Das Modul erklärt die Ausbreitungsrichtung, die korrekte Positionierung
von `contextWrite()` und typische Anwendungsfälle wie Request-IDs und Auth-Tokens.

**Kernthemen:** `contextWrite()`, `deferContextual()`, `ContextView`,
Ausbreitungsrichtung, Schichtung von Context-Einträgen.

---

### 12 – Debugging *(Pipelines beobachten)*

In asynchronem Code sind Stack-Traces wenig hilfreich – sie zeigen Reactor-Interna,
aber nicht die eigene Codezeile, die das Problem verursacht hat. Dieses Modul stellt
alle Debugging-Werkzeuge vor: `.log()` für alle Reactive-Streams-Signale, `doOn*` für
gezielte Side-Effects, `.checkpoint()` zum Einrahmen von Fehlerquellen und
`Hooks.onOperatorDebug()` (nur Entwicklung, hohe Laufzeitkosten) für vollständige
Assembly-Traces.

**Kernthemen:** `.log()`, `doOnNext/Error/Complete/Each()`, `.checkpoint()`,
`.tap()`, `Hooks.onOperatorDebug()`, `ReactorDebugAgent`.

---

### 13 – BufferWindowGroup *(Elemente gruppieren)*

Die drei Operatoren für Batch-Verarbeitung und Gruppierung: `buffer()` sammelt
Elemente in `List<T>`-Batches, `window()` öffnet pro Gruppe einen eigenen
`Flux<T>` (Streaming ohne Speicher-Overhead), `groupBy()` teilt den Stream nach
einem Schlüssel in parallele `GroupedFlux`-Streams auf. Das Modul zeigt sowohl
zählerbasierte als auch zeitbasierte Varianten.

**Kernthemen:** `buffer(n)`, `buffer(size, skip)`, `buffer(Duration)`,
`window(n)`, `window(Duration)`, `groupBy()`, `GroupedFlux`.

---

## Voraussetzungen

- Java 17 oder höher (Modul 01 benötigt Java 9+ für `java.util.concurrent.Flow`)
- Gradle (ab Modul 02, Wrapper ist jeweils enthalten)
- IntelliJ IDEA oder eine andere Java-IDE

## Starten eines Moduls

```bash
cd 02_ReactorBasics
./gradlew run
```

Modul 01 wird direkt über die IDE oder `javac`/`java` ausgeführt (kein Gradle).
Modul 09 (Testing) läuft mit `./gradlew test`.
