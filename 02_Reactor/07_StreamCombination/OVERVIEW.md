# 07_StreamCombination – Mehrere Publisher zusammenführen

## Übersicht

Reaktive Anwendungen arbeiten selten mit einem einzelnen Stream. Häufig müssen
mehrere Publisher kombiniert werden – sei es sequenziell, parallel oder
paarweise. Reactor stellt dafür vier Kernoperatoren bereit:

| Operator | Abonnement | Reihenfolge | Wartet auf alle? |
|---|---|---|---|
| `concat()` | nacheinander | garantiert | Ja (erst nach Abschluss der Vorgänger) |
| `merge()` | gleichzeitig | nach Ankunft | Nein |
| `zip()` | gleichzeitig | paarweise | Ja (auf nächstes Element jeder Quelle) |
| `combineLatest()` | gleichzeitig | neuestes Paar | Nein |

---

## concat() – sequenziell

`concat()` abonniert den nächsten Publisher erst, wenn der vorherige
abgeschlossen (`onComplete`) hat. Die Reihenfolge der Elemente ist garantiert.

```java
Flux<String> quelle1 = Flux.just("A", "B", "C");
Flux<String> quelle2 = Flux.just("X", "Y", "Z");

Flux.concat(quelle1, quelle2)
    .subscribe(System.out::println);
// A B C X Y Z  – Reihenfolge garantiert
```

**Wann verwenden?**
- Seiten oder Chunks nacheinander laden
- Quelle 2 darf erst starten, wenn Quelle 1 fertig ist
- Reihenfolge ist fachlich wichtig

**Achtung:** Wenn Quelle 1 nie abschließt (z.B. unendlicher Flux), wird
Quelle 2 niemals abonniert.

---

## merge() – parallel, nach Ankunft

`merge()` abonniert alle Quellen gleichzeitig und leitet Elemente sofort
weiter, sobald sie ankommen – unabhängig von der Quellen-Reihenfolge.

```java
Flux<String> schnell = Flux.just("schnell-1", "schnell-2")
    .delayElements(Duration.ofMillis(100));

Flux<String> langsam = Flux.just("langsam-1", "langsam-2")
    .delayElements(Duration.ofMillis(250));

Flux.merge(schnell, langsam)
    .subscribe(System.out::println);
// schnell-1, schnell-2, langsam-1, schnell-... (zeitabhängig)
```

**Wann verwenden?**
- Mehrere unabhängige Quellen parallel konsumieren
- Durchsatz ist wichtiger als Reihenfolge
- Typisch: mehrere WebSocket-Kanäle, Event-Quellen

### concat() vs. merge() im Überblick

```
concat():  [Quelle1 komplett]──[Quelle2 komplett]──▶
merge():   [Quelle1]
           [Quelle2]          ──▶ (vermischt nach Zeitpunkt)
```

---

## zip() – paarweise kombinieren

`zip()` wartet auf je ein Element von jeder Quelle und kombiniert sie.
Das Tempo bestimmt die langsamste Quelle. Wenn eine Quelle endet, stoppt
der gesamte Flux.

```java
Flux<String> vornamen  = Flux.just("Anna", "Ben", "Clara");
Flux<String> nachnamen = Flux.just("Müller", "Schmidt", "Weber");

// Mit Tuple2:
Flux.zip(vornamen, nachnamen)
    .subscribe(t -> System.out.println(t.getT1() + " " + t.getT2()));

// Mit Combinator-Funktion:
Flux.zip(vornamen, nachnamen, (v, n) -> v + " " + n)
    .subscribe(System.out::println);
// Anna Müller, Ben Schmidt, Clara Weber
```

**Wann verwenden?**
- Elemente aus mehreren Quellen gehören logisch zusammen (gleicher Index)
- Vorname-Liste + Nachname-Liste → vollständige Namen
- API-Ergebnis + Metadaten → kombiniertes Objekt

**Kürzeste Quelle gewinnt:** Hat eine Quelle 3 Elemente, eine andere nur 2,
stoppt zip() nach 2 Paaren.

---

## combineLatest() – immer das neueste Paar

`combineLatest()` kombiniert bei jeder neuen Emission das neueste Element
mit den zuletzt bekannten Werten der anderen Quellen.

```java
Flux<String> temperatur = Flux.just("20°C", "22°C", "25°C")
    .delayElements(Duration.ofMillis(100));

Flux<String> luftdruck = Flux.just("1013 hPa", "1010 hPa")
    .delayElements(Duration.ofMillis(200));

Flux.combineLatest(temperatur, luftdruck, (t, l) -> t + " / " + l)
    .subscribe(System.out::println);
// 20°C / 1013 hPa
// 22°C / 1013 hPa   ← neue Temp, alter Luftdruck
// 25°C / 1013 hPa
// 25°C / 1010 hPa   ← alter Temp, neuer Luftdruck
```

**Wann verwenden?**
- UI-Formulare: Feldwert A + Feldwert B → Live-Validierung
- Sensordaten: immer das neueste Paar aus zwei Sensoren
- Konfiguration + Zustand: reagiere auf jede Änderung

### zip() vs. combineLatest()

| | `zip()` | `combineLatest()` |
|---|---|---|
| Wartet auf | nächstes Element jeder Quelle | irgendeine neue Emission |
| Emissionen | so viele wie die kürzeste Quelle | jede neue Emission triggert |
| Reihenfolge | strikt paarweise (Index 0, 1, 2…) | immer aktuelle Kombination |
| Use-Case | zusammengehörige Listen | Live-Daten, Formulare |

---

## Instanzmethoden: concatWith(), mergeWith(), zipWith()

Neben den statischen Methoden gibt es Instanzmethoden für die fluent API:

```java
// Statt: Flux.concat(a, b)
a.concatWith(b)

// Statt: Flux.merge(a, b)
a.mergeWith(b)

// Statt: Flux.zip(a, b, combinator)
a.zipWith(b, combinator)
```

Sie sind funktional identisch – nur syntaktisch besser in eine Operator-Kette
eingebaut:

```java
Flux.just("Montag", "Dienstag", "Mittwoch")
    .map(String::toUpperCase)
    .zipWith(Flux.range(1, 3), (tag, nr) -> nr + ". " + tag)
    .subscribe(System.out::println);
// 1. MONTAG, 2. DIENSTAG, 3. MITTWOCH
```

---

## Entscheidungsbaum: Welcher Kombinations-Operator?

```
Mehrere Publisher zusammenführen?
     │
     ├─ Reihenfolge garantiert, Quelle 2 erst nach Quelle 1?
     │       └─ concat() / concatWith()
     │
     ├─ Parallel, Elemente nach Ankunft?
     │       └─ merge() / mergeWith()
     │
     ├─ Elemente paarweise zusammenführen (gleicher Index)?
     │       └─ zip() / zipWith()
     │
     └─ Immer neueste Kombination bei jeder neuen Emission?
             └─ combineLatest()
```

---

## Beispiele in `Main.java`

| Beispiel | Thema |
|---|---|
| 1 | `concat()` – sequenziell, Reihenfolge garantiert |
| 2 | `merge()` – parallel, Reihenfolge nach Ankunft |
| 3 | `concat()` vs. `merge()` – synchroner Vergleich |
| 4 | `zip()` – paarweises Kombinieren mit Tuple und Combinator |
| 5 | `combineLatest()` – immer das neueste Paar |
| 6 | `concatWith()` / `mergeWith()` – Instanzmethoden |
| 7 | `zipWith()` – Instanzmethode in der Operator-Kette |

---

## Projektstruktur

```
07_StreamCombination/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/streamcombination/
│   └── Main.java             ← Alle 7 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 06 vs. 07

| | 06_HotColdPublishers | 07_StreamCombination |
|---|---|---|
| Kernfrage | Wer bekommt welche Elemente? | Wie führt man mehrere Quellen zusammen? |
| Subscriber-Rolle | zentral | weniger relevant |
| Operator-Fokus | publish, share, cache, Sinks | concat, merge, zip, combineLatest |
| Timing | Hot vs. Cold | Sequenziell vs. Parallel |

---

## Nächste Schritte

Das nächste Modul deckt **Backpressure** ab: Was passiert, wenn der Subscriber
langsamer konsumiert als der Publisher produziert?
`onBackpressureBuffer()`, `onBackpressureDrop()`, `limitRate()`.
