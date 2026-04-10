# 08_Backpressure – Fluss zwischen Publisher und Subscriber steuern

## Übersicht

Backpressure beschreibt das Problem, wenn ein Publisher schneller Elemente
produziert als der Subscriber verarbeiten kann. Das Reactive-Streams-Protokoll
löst das durch `request(n)`: Der Subscriber teilt dem Publisher mit, wie viele
Elemente er verarbeiten kann.

Reactor stellt mehrere Strategien bereit:

| Strategie | Datenverlust? | Speicher | Wann verwenden? |
|---|---|---|---|
| `request(n)` / `BaseSubscriber` | Nein | kontrolliert | eigener Verarbeitungsrhythmus |
| `limitRate(n)` | Nein | begrenzt | Pagination, Datenbankabfragen |
| `onBackpressureBuffer()` | Nein | unbegrenzt! | kurze Bursts, Subscriber holt auf |
| `onBackpressureBuffer(n)` | Nein / Error | begrenzt | Fail-fast bei Überlastung |
| `onBackpressureDrop()` | Ja | keiner | Metriken, Sampling, nicht-kritische Daten |
| `onBackpressureLatest()` | Ja (bis auf letztes) | keiner | Cursor-Position, aktuelle Preise |

---

## Wann entsteht Backpressure?

Der Konflikt tritt auf, wenn Publisher und Subscriber **auf verschiedenen Threads** laufen:

```
Publisher (Thread A) → produziert schnell
Subscriber (Thread B) → verarbeitet langsam

Ohne Backpressure-Strategie: MissingBackpressureException
```

Mit `publishOn()` wird der Subscriber in einen anderen Thread verschoben –
genau hier kann Backpressure sichtbar werden.

---

## BaseSubscriber – manuelles request()

Der direkteste Weg: eigene Implementierung von `BaseSubscriber<T>`.

```java
Flux.range(1, 10)
    .subscribe(new BaseSubscriber<Integer>() {
        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            request(1);  // erstes Element anfordern
        }

        @Override
        protected void hookOnNext(Integer value) {
            System.out.println("Empfangen: " + value);
            request(1);  // nächstes Element anfordern
        }
    });
```

**Wann verwenden?**
- Exakte Steuerung des Verarbeitungsrhythmus
- Erst nach Datenbankschreiben das nächste Element anfordern

---

## limitRate() – Prefetch-Batches

`limitRate(n)` teilt dem Publisher mit, dass Reactor intern maximal `n`
Elemente auf einmal anfordern soll. Nachgefüllt wird, wenn 75% verbraucht sind.

```java
Flux.range(1, 1000)
    .limitRate(50)       // maximal 50 auf einmal vorausproduzierten
    .publishOn(Schedulers.boundedElastic())
    .subscribe(System.out::println);
```

**Wann verwenden?**
- Datenbankabfragen mit Cursor/Pagination
- Speicherkontrolle bei großen Streams

---

## onBackpressureBuffer()

Puffert alle Elemente, die der Subscriber noch nicht angefordert hat.
Ohne Limit kann der Puffer beliebig groß werden.

```java
Flux.range(1, 1000)
    .onBackpressureBuffer()
    .publishOn(Schedulers.boundedElastic())
    .subscribe(n -> verarbeite(n));
```

Mit Limit und Overflow-Handler:

```java
Flux.range(1, 1000)
    .onBackpressureBuffer(
        100,                           // max. 100 Elemente
        verworfen -> log(verworfen)    // Handler pro verworfenem Element
    )
    .publishOn(Schedulers.boundedElastic())
    .subscribe(...);
```

---

## onBackpressureDrop()

Verwirft Elemente, die der Subscriber gerade nicht anfordern kann.
Kein Fehler, aber Datenverlust.

```java
Flux.range(1, 1000)
    .onBackpressureDrop(verworfen -> System.out.println("DROP: " + verworfen))
    .publishOn(Schedulers.boundedElastic())
    .subscribe(n -> verarbeite(n));
```

---

## onBackpressureLatest()

Wie Drop, aber der zuletzt gesehene Wert bleibt immer erhalten.
Wenn der Subscriber wieder bereit ist, bekommt er das neueste Element.

```java
Flux.range(1, 1000)
    .onBackpressureLatest()
    .publishOn(Schedulers.boundedElastic())
    .subscribe(n -> verarbeite(n));
```

---

## Entscheidungsbaum: Welche Strategie?

```
Backpressure-Strategie wählen
         │
         ├─ Kein Datenverlust erlaubt?
         │       ├─ Pagination / Batch-Größe steuern? → limitRate(n)
         │       ├─ Kurze Bursts puffern?             → onBackpressureBuffer()
         │       └─ Fail-fast bei Überlastung?        → onBackpressureBuffer(n)
         │
         └─ Datenverlust akzeptabel?
                 ├─ Nur neuester Wert relevant?       → onBackpressureLatest()
                 └─ Alle verpassten ignorieren?       → onBackpressureDrop()
```

---

## Beispiele in `Main.java`

| Beispiel | Thema |
|---|---|
| 1 | `BaseSubscriber` – manuelles `request(1)` Element für Element |
| 2 | `limitRate()` – Prefetch-Batches steuern |
| 3 | `onBackpressureBuffer()` – unbegrenzter Puffer |
| 4 | `onBackpressureBuffer(n)` – Puffer mit Limit und Overflow-Handler |
| 5 | `onBackpressureDrop()` – Elemente verwerfen mit Handler |
| 6 | `onBackpressureLatest()` – nur neuestes Element vorhalten |
| 7 | Strategievergleich – Übersichtstabelle |

---

## Projektstruktur

```
08_Backpressure/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/backpressure/
│   └── Main.java             ← Alle 7 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 07 vs. 08

| | 07_StreamCombination | 08_Backpressure |
|---|---|---|
| Kernfrage | Wie führt man mehrere Quellen zusammen? | Wie reguliert man den Fluss? |
| Subscriber-Rolle | passiv | aktiv (request) |
| Operator-Fokus | concat, merge, zip, combineLatest | onBackpressure*, limitRate, BaseSubscriber |
| Problem | Koordination | Geschwindigkeitsunterschied |

---

## Nächste Schritte

Das nächste Modul deckt **Testing** ab: Wie testet man reaktive Streams?
`StepVerifier`, `TestPublisher`, virtuelle Zeit mit `VirtualTimeScheduler`.
