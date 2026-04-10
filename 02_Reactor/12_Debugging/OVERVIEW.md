# Modul 12: Debugging

## Lernziele

- Reaktive Pipelines mit `.log()` beobachten
- Side-Effects mit `doOn*`-Operatoren einbauen
- Fehlerquellen mit `.checkpoint()` eingrenzen
- Den Unterschied zwischen `.log()`, `doOn*` und `.tap()` erklären
- `Hooks.onOperatorDebug()` gezielt einsetzen (und dessen Kosten kennen)

---

## Das Problem: Fehlende Stack-Traces

In synchronem Java zeigt ein Stack-Trace, wo der Fehler **aufgetreten** ist.
In Reactor wissen wir das auch – aber nicht, wo die Pipeline **aufgebaut** wurde:

```
Exception in thread "main":
  at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(...)
  at reactor.core.publisher.FluxArray$ArraySubscription.fastPath(...)
  ... (nur interner Reactor-Code)
```

Es fehlt der Hinweis auf **unsere** Codezeile, die den fehlerhaften `map()`
erstellt hat.

---

## Werkzeuge im Überblick

| Werkzeug                  | Kosten    | Einsatz                                        |
|---------------------------|-----------|------------------------------------------------|
| `.log()`                  | gering    | Alle Signale sichtbar machen (Entwicklung)     |
| `doOnNext/Error/Complete` | sehr ger. | Einzelne Signale abfangen (auch Produktion)    |
| `.tap()`                  | sehr ger. | Mehrere doOn*-Callbacks gebündelt              |
| `.checkpoint()`           | gering    | Fehlerort in der Pipeline einrahmen            |
| `Hooks.onOperatorDebug()` | hoch      | Globales Assembly-Tracing (nur Entwicklung!)   |
| `ReactorDebugAgent`       | gering    | Java-Agent: Assembly-Traces ohne Laufzeitkosten|

---

## .log() – alle Reactive-Streams-Signale

`.log()` protokolliert alle Signale, die durch diesen Punkt der Pipeline fließen:

```
reactor.Flux.Array.1: | onSubscribe([Synchronous Fuseable] FluxArray.ArraySubscription)
reactor.Flux.Array.1: | request(unbounded)
reactor.Flux.Array.1: | onNext(A)
reactor.Flux.Array.1: | onNext(B)
reactor.Flux.Array.1: | onComplete()
```

Mit Kategorie:
```java
.log("NACH_MAP")          // Präfix im Log
.log("FILTER", Level.FINE) // nur bei FINE-Level ausgeben
```

---

## .checkpoint() – Fehlerort eingrenzen

`.checkpoint()` fügt beim Fehler einen Marker in den Stack-Trace ein,
der auf die Assembly-Stelle hinweist – ohne globale Hooks:

```java
Flux<Integer> pipeline = Flux.just(5, 0, 3)
    .checkpoint("vor-Division")
    .map(n -> 10 / n)
    .checkpoint("nach-Division");
```

Fehler-Ausgabe enthält dann:
```
Assembly trace from producer [FluxMap], described as [nach-Division] :
  de.debugging.Main.meinePipeline(Main.java:42)
```

Mit vollständigem Stack-Trace (teurer):
```java
.checkpoint("beschreibung", true)
```

---

## doOn*-Operatoren

| Operator             | Feuert bei                                    |
|----------------------|-----------------------------------------------|
| `doOnSubscribe()`    | Subscription erstellt                         |
| `doOnRequest()`      | `request(n)` vom Subscriber                   |
| `doOnNext()`         | jedem emittierten Element                     |
| `doOnError()`        | Fehler-Signal                                 |
| `doOnComplete()`     | Abschluss-Signal                              |
| `doOnTerminate()`    | Complete oder Error                           |
| `doAfterTerminate()` | Nach Terminate (auch Cancel)                  |
| `doOnCancel()`       | Subscriber cancelt                            |
| `doFinally()`        | Immer am Ende (Complete, Error oder Cancel)   |

---

## doOnEach() – alle Signale in einem Callback

`doOnEach()` liefert jedes Signal als `Signal<T>`-Objekt und erlaubt,
alle Signaltypen in einem einzigen Lambda zu behandeln:

```java
.doOnEach(signal -> {
    if (signal.isOnNext())     { System.out.println("onNext: " + signal.get()); }
    if (signal.isOnError())    { System.out.println("onError: " + signal.getThrowable()); }
    if (signal.isOnComplete()) { System.out.println("onComplete"); }
})
```

Besonderheit: `Signal<T>` enthält auch den Context (`signal.getContextView()`),
was `doOnEach()` besonders nützlich für Logging mit Korrelations-IDs macht.

Hinweis: `.tap()` ist eine instrumentation-orientierte Alternative für
Bibliotheken (z.B. Micrometer-Integration), die ein `SignalListenerFactory`-
Interface implementieren wollen.

---

## Hooks.onOperatorDebug()

```java
Hooks.onOperatorDebug();   // EINMAL aktivieren, bevor Pipelines aufgebaut werden
// ... Pipelines aufbauen und subscriben ...
Hooks.resetOnOperatorDebug(); // nach dem Test wieder deaktivieren
```

**ACHTUNG:** Hooks ist global und hat **hohe Laufzeitkosten** (Stack-Capture
bei jedem Operator-Assembly). Nur für Entwicklung und Diagnose verwenden.

---

## ReactorDebugAgent (empfohlen für Produktion)

`ReactorDebugAgent` ist ein Java-Agent, der Assembly-Traces per
Bytecode-Instrumentation einfügt – ohne Laufzeitkosten:

```java
// Einmalig beim Programmstart:
ReactorDebugAgent.init();
ReactorDebugAgent.processExistingClasses();
```

Abhängigkeit in `build.gradle.kts`:
```kotlin
implementation("io.projectreactor:reactor-tools:3.7.4")
```

---

## Beispiele in diesem Modul

| Beispiel | Thema                          | Neue APIs                                                    |
|----------|--------------------------------|--------------------------------------------------------------|
| 1        | `.log()` Grundlagen            | Alle Reactive-Streams-Signale sichtbar                       |
| 2        | `.log()` mit Kategorie         | Mehrere log()-Punkte in einer Pipeline                       |
| 3        | `doOn*`-Operatoren             | doOnSubscribe, doOnNext, doOnError, doOnComplete             |
| 4        | `.checkpoint()` Grundlagen     | Fehlerort ohne Beschreibung markieren                        |
| 5        | `.checkpoint()` mit Text       | Beschreibenden Marker setzen                                 |
| 6        | `Hooks.onOperatorDebug()`      | Globales Assembly-Tracing aktivieren/deaktivieren            |
| 7        | `doOnEach()`                   | `Signal<T>`, `isOnNext/Error/Complete()`, Context-Zugriff   |

---

## Projektstruktur

```
12_Debugging/
├── build.gradle.kts        (enthält reactor-tools für ReactorDebugAgent)
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── src/
    └── main/
        └── java/
            └── de/debugging/
                └── Main.java
```

**Starten:**
```bash
./gradlew run
```

---

## Vergleich: 11_Context → 12_Debugging

| Aspekt            | 11_Context                        | 12_Debugging                         |
|-------------------|-----------------------------------|--------------------------------------|
| Thema             | Metadaten durch Pipeline leiten   | Fehler und Signale beobachten        |
| Sichtbarkeit      | Datenpfad (was fließt)            | Signal-Pfad (wie es fließt)          |
| Hauptwerkzeug     | `contextWrite`, `deferContextual` | `log()`, `checkpoint()`, `doOn*`     |
| Typischer Einsatz | Korrelations-IDs, Auth-Token      | Diagnose, Monitoring, Alerting       |

---

## Nächste Schritte

→ **13_BufferWindowGroup**: Wie fasst man Elemente eines Flux zu Gruppen
  zusammen? `buffer()`, `window()` und `groupBy()` im Vergleich.
