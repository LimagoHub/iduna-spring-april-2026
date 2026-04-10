# Modul 13: Buffer, Window und GroupBy

## Lernziele

- Den Unterschied zwischen `buffer()`, `window()` und `groupBy()` erklären
- `buffer(n)` und `buffer(Duration)` für Batch-Verarbeitung einsetzen
- Überlappende und lückige Puffer mit `buffer(size, skip)` erzeugen
- `window()` als `Flux<Flux<T>>` verstehen und innere Fluxe verarbeiten
- `groupBy()` zum Aufteilen nach Schlüsseln nutzen

---

## Überblick: Die drei Operatoren

```
Eingabe: Flux<T>  →  1 2 3 4 5 6 7 8 9 ...

buffer(3):   [1,2,3]  [4,5,6]  [7,8,9]       → Flux<List<T>>
window(3):   Flux[1,2,3]  Flux[4,5,6]  ...   → Flux<Flux<T>>
groupBy(n%2==0 ? "gerade" : "ungerade"):
  "ungerade" → 1 3 5 7 9 ...                 → Flux<GroupedFlux<K,T>>
  "gerade"   → 2 4 6 8 ...
```

---

## buffer() – Elemente in Listen sammeln

### Varianten

| Signatur                       | Beschreibung                                             |
|--------------------------------|----------------------------------------------------------|
| `buffer(int maxSize)`          | Je `maxSize` Elemente zu einer Liste                     |
| `buffer(Duration timespan)`    | Alle Elemente innerhalb des Zeitfensters                 |
| `buffer(int size, int skip)`   | Feste Größe, skip definiert Abstand zwischen Starts     |
| `buffer(Publisher boundary)`   | Neuer Puffer bei jedem Signal des Boundary-Publishers   |

### buffer(size, skip) – Muster

```
size=3, skip=1  → überlappend:  [1,2,3] [2,3,4] [3,4,5] ...
size=3, skip=3  → normal:       [1,2,3] [4,5,6] [7,8,9] ...
size=2, skip=3  → lückig:       [1,2]   [4,5]   [7,8]   ... (3 wird übersprungen)
```

---

## window() – Flux von Flux

`window()` gibt `Flux<Flux<T>>` zurück. Jedes innere `Flux<T>` repräsentiert
ein Zeitfenster oder eine Gruppe.

**Wichtig:** Jedes innere Flux **muss subscribet** werden – sonst werden die
Elemente ignoriert. `flatMap()` ist das übliche Muster:

```java
Flux.range(1, 9)
    .window(3)
    .flatMap(fenster -> fenster.reduce(0, Integer::sum))  // Summe pro Fenster
    .subscribe(System.out::println);  // 6  15  24
```

### Unterschied buffer() vs. window()

| Eigenschaft          | `buffer(n)`                         | `window(n)`                      |
|----------------------|-------------------------------------|----------------------------------|
| Ausgabe              | `Flux<List<T>>`                     | `Flux<Flux<T>>`                  |
| Speicher             | Alle Elemente erst am Ende          | Streaming: sofort verfügbar      |
| Backpressure         | Puffer wächst im Speicher           | Backpressure durch inneren Flux  |
| Geeignet für         | Kompakte Batch-Verarbeitung         | Streaming-Aggregation            |

---

## groupBy() – nach Schlüssel aufteilen

```java
Flux.range(1, 8)
    .groupBy(n -> n % 2 == 0 ? "gerade" : "ungerade")
    .flatMap(gruppe -> gruppe.collectList()
        .map(liste -> gruppe.key() + ": " + liste))
    .subscribe(System.out::println);
```

`GroupedFlux<K, T>` hat:
- `.key()` – den Schlüssel dieser Gruppe
- Alle `Flux<T>`-Operatoren (ist selbst ein `Flux<T>`)

### Mit valueMapper

```java
.groupBy(
    s -> s.length(),          // Schlüssel: Länge des Strings
    s -> s.toUpperCase()      // Wert: Großbuchstaben
)
```

### Wichtige Einschränkung

`groupBy()` erstellt für jede neue Schlüssel-Gruppe einen neuen `GroupedFlux`.
Wenn der `flatMap()` nicht schnell genug subscribed oder innere Fluxe nicht
konsumiert werden, kann die Anzahl offener Gruppen die Default-Prefetch-Grenze
überschreiten → `IllegalStateException: Too many groups`.

Lösung: `concatMap()` statt `flatMap()` oder `groupBy(keySelector, maxConcurrency)`.

---

## Beispiele in diesem Modul

| Beispiel | Thema                            | Neue APIs                                          |
|----------|----------------------------------|----------------------------------------------------|
| 1        | `buffer(n)` – feste Grösse       | Basis-Buffer, letzter Puffer kürzer                |
| 2        | `buffer(size, skip)`             | Überlappende und lückige Puffer                    |
| 3        | `buffer(Duration)`               | Zeitbasierter Puffer mit `Flux.interval()`         |
| 4        | `window(n)`                      | `Flux<Flux<T>>`, `flatMap` + `reduce`              |
| 5        | `window(Duration)`               | Zeitfenster mit `.index()`                         |
| 6        | `groupBy()` – Grundlagen         | `GroupedFlux`, `.key()`, `collectList()`           |
| 7        | `groupBy()` mit Aggregation      | `count()`, Sortierung, `valueMapper`               |

---

## Projektstruktur

```
13_BufferWindowGroup/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── src/
    └── main/
        └── java/
            └── de/bufferwindowgroup/
                └── Main.java
```

**Starten:**
```bash
./gradlew run
```

---

## Vergleich: 12_Debugging → 13_BufferWindowGroup

| Aspekt            | 12_Debugging                        | 13_BufferWindowGroup                 |
|-------------------|-------------------------------------|--------------------------------------|
| Thema             | Fehler und Signale beobachten       | Elemente zu Gruppen zusammenfassen   |
| Ausgabe           | Gleicher Typ, Side-Effects          | Aggregierter Typ (List, Flux, Group) |
| Hauptwerkzeug     | `log()`, `checkpoint()`, `doOn*`    | `buffer()`, `window()`, `groupBy()`  |
| Backpressure      | transparent                         | bei window/groupBy kritisch          |

---

## Kursabschluss

Mit diesem Modul sind alle grundlegenden Bereiche von Reactor abgedeckt:

| Modul | Thema                    |
|-------|--------------------------|
| 01    | Java 9 Flow API          |
| 02    | Reactor Basics           |
| 03    | Schedulers               |
| 04    | FlatMap                  |
| 05    | Error Handling           |
| 06    | Hot & Cold Publishers    |
| 07    | Stream Combination       |
| 08    | Backpressure             |
| 09    | Testing                  |
| 10    | Sinks                    |
| 11    | Context                  |
| 12    | Debugging                |
| **13**| **Buffer, Window, GroupBy** |
