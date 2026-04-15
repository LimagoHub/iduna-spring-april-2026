# Übungen – Stream-Kombination mit f1 und f2

Die beiden Flux-Objekte aus `Main.java` sind die Grundlage aller Aufgaben:

```java
Flux<String> f1  // Vornamen aus vornamen.txt:  John, Max, John Boy, Dschingis
Flux<String> f2  // Nachnamen aus nachnamen.txt: Doe, Mustermann, Walton, Kahn
```

Alle Lösungen befinden sich in `Uebungen.java` (Package `de.flatmap`).

---

## Aufgabe 1 – `concat()`: Vornamen und Nachnamen sequenziell zusammenführen

Verbinde `f1` und `f2` mit `Flux.concat()`.

**Erwartete Ausgabe (Reihenfolge garantiert):**
```
John
Max
John Boy
Dschingis
Doe
Mustermann
Walton
Kahn
```

**Lernziel:** `concat()` abonniert `f2` erst, wenn `f1` vollständig abgeschlossen ist.

---

## Aufgabe 2 – `merge()`: Parallel zusammenführen mit Verzögerung

Füge `f1` und `f2` Verzögerungen hinzu (`f1` = 100 ms, `f2` = 150 ms)
und führe beide mit `Flux.merge()` zusammen.

**Erwartete Ausgabe (zeitabhängig, typischerweise):**
```
John
Doe
Max
Mustermann
John Boy
Walton
Dschingis
Kahn
```

**Lernziel:** `merge()` abonniert beide Quellen gleichzeitig. Die Reihenfolge
hängt vom Timing ab – nicht von der Quellen-Reihenfolge.

---

## Aufgabe 3 – `zip()` mit Tuple: Vorname + Nachname als Paar

Verbinde `f1` und `f2` mit `Flux.zip()` **ohne Combinator-Funktion**
und gib jedes Paar als `(Vorname, Nachname)` aus (Zugriff via `t.getT1()` / `t.getT2()`).

**Erwartete Ausgabe:**
```
(John, Doe)
(Max, Mustermann)
(John Boy, Walton)
(Dschingis, Kahn)
```

**Lernziel:** `zip()` kombiniert Elemente mit demselben Index zu einem `Tuple2`.

---

## Aufgabe 4 – `zip()` mit Combinator: Vollständige Namen bilden

Verbinde `f1` und `f2` mit `Flux.zip()` und einer **Combinator-Funktion**,
die direkt den vollständigen Namen (`"John Doe"`) erzeugt – kein Tuple.

**Erwartete Ausgabe:**
```
John Doe
Max Mustermann
John Boy Walton
Dschingis Kahn
```

**Lernziel:** Der dritte Parameter von `zip()` ist eine BiFunction – sie ersetzt das Tuple durch einen direkten Ergebniswert.

---

## Aufgabe 5 – `combineLatest()`: Immer neueste Kombination

Füge `f1` 100 ms Verzögerung und `f2` 250 ms Verzögerung hinzu.
Verwende `Flux.combineLatest()` und beobachte, wie jede neue Emission
sofort mit dem letzten bekannten Wert der anderen Quelle kombiniert wird.

**Erwartete Ausgabe (ungefähr):**
```
Max Doe
John Boy Doe
Dschingis Doe
Dschingis Mustermann
Dschingis Walton
Dschingis Kahn
```

**Lernziel:** `combineLatest()` reagiert auf jede einzelne Emission – mehr Ausgaben als `zip()`,
dafür immer der aktuellste Zustand beider Quellen.

---

## Aufgabe 6 – `concatWith()`: Instanzmethode statt statischer Methode

Verbinde `f1` und `f2` mit der **Instanzmethode** `concatWith()` statt `Flux.concat()`.

**Erwartete Ausgabe:** identisch zu Aufgabe 1.

**Lernziel:** `concatWith()` ist die fluent-API-Variante von `Flux.concat()` –
funktional identisch, aber eleganter in einer Operator-Kette.

---

## Aufgabe 7 – `zipWith()`: Instanzmethode mit vorgeschaltetem `map()`

Wandle `f1` zunächst mit `.map(String::toUpperCase)` in Großbuchstaben um
und verwende dann die Instanzmethode `zipWith(f2, combinator)`,
um Namen im Format `"Nachname, VORNAME"` zu erzeugen.

**Erwartete Ausgabe:**
```
Doe, JOHN
Mustermann, MAX
Walton, JOHN BOY
Kahn, DSCHINGIS
```

**Lernziel:** `zipWith()` kann direkt nach einem anderen Operator eingebaut werden,
ohne die Operator-Kette zu unterbrechen.

---

## Aufgabe 8 – `zip()` vs. `combineLatest()`: Direkter Vergleich

Verwende denselben Datensatz (`f1` = 100 ms, `f2` = 300 ms) einmal mit `zip()`
und einmal mit `combineLatest()`.

**Erwartete Ausgabe `zip()` (4 Paare, langsamere Quelle bestimmt Tempo):**
```
John Doe
Max Mustermann
John Boy Walton
Dschingis Kahn
```

**Erwartete Ausgabe `combineLatest()` (mehr Ausgaben):**
```
Max Doe
John Boy Doe
Dschingis Doe
Dschingis Mustermann
Dschingis Walton
Dschingis Kahn
```

**Lernziel:**
- `zip()` → wartet auf ein neues Element aus **beiden** Quellen (paarweise)
- `combineLatest()` → reagiert auf **jede** neue Emission mit dem letzten bekannten Wert

---

## Lösungen

```
src/main/java/de/flatmap/Uebungen.java
```
