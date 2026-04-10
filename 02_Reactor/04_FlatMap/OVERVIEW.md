# 04_FlatMap – Der wichtigste Reaktiv-Operator

## Übersicht

Dieses Modul erklärt `flatMap` – den zentralen Operator in Project Reactor und
reaktiver Programmierung generell. Er ist das Bindeglied zwischen synchronen
Datenpipelines und echter asynchroner Komposition.

---

## map vs. flatMap

Der Schlüsselunterschied:

| Operator | Signatur | Verwendung |
|---|---|---|
| `map()` | `T → R` | Synchrone 1:1-Transformation |
| `flatMap()` | `T → Publisher<R>` | Asynchrone Transformation, ggf. 1:n |

```java
// map: jeder Wert wird direkt transformiert
Flux.just(1, 2, 3)
    .map(n -> n * 10)
    // → 10, 20, 30

// flatMap: jeder Wert wird zu einem Publisher expandiert
Flux.just(1, 2, 3)
    .flatMap(n -> Flux.just(n * 10, n * 10 + 1))
    // → 10, 11, 20, 21, 30, 31
```

**Faustregel:** Wenn die Transformation einen `Mono` oder `Flux` zurückgibt
(z.B. eine DB-Abfrage oder ein HTTP-Call), braucht man `flatMap`.

---

## flatMap für asynchrone Operationen

Das häufigste Muster in reaktiven Anwendungen:

```java
Flux.just("Alice", "Bob", "Carol")
    .flatMap(name -> ladeBenutzerprofil(name))  // String → Mono<String>
    .subscribe(profil -> System.out.println(profil));

Mono<String> ladeBenutzerprofil(String name) {
    return Mono.fromCallable(() -> "Profil[" + name + ", Rolle=Admin]");
}
```

`Mono.fromCallable()` kapselt beliebigen Code (inkl. Exceptions) in einen `Mono`.

---

## flatMap und Parallelismus

`flatMap()` abonniert alle Sub-Publisher **sofort** – nicht nacheinander.
Wenn jeder `Mono` auf `Schedulers.boundedElastic()` läuft, entstehen echte
parallele Ausführungen:

```java
Flux.just("Service-A", "Service-B", "Service-C")
    .flatMap(service ->
        Mono.fromCallable(() -> {
            Thread.sleep(200); // simulierter HTTP-Call
            return service + ": OK";
        }).subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
```

```
main-Thread:
  flatMap startet Sub-Mono für Service-A  →  boundedElastic-1: 200ms
  flatMap startet Sub-Mono für Service-B  →  boundedElastic-2: 200ms
  flatMap startet Sub-Mono für Service-C  →  boundedElastic-3: 200ms
                                              ↓ alle drei gleichzeitig
  Gesamtdauer ≈ 200ms  (statt 3 × 200ms = 600ms)
```

---

## flatMap vs. concatMap vs. switchMap

Alle drei nehmen `T → Publisher<R>`, unterscheiden sich aber im Verhalten:

| Operator | Reihenfolge | Parallelismus | Typischer Use-Case |
|---|---|---|---|
| `flatMap()` | unbestimmt | ja | Parallele HTTP-Calls, DB-Abfragen |
| `concatMap()` | garantiert | nein | Sequentielle Inserts, abhängige Schritte |
| `switchMap()` | nur letzter | ja | Typeahead-Suche, Autocomplete |

### concatMap – reihenfolgeerhaltend

```java
Flux.just("Schritt-1", "Schritt-2", "Schritt-3")
    .concatMap(schritt ->
        Mono.fromCallable(() -> {
            // Schritt-2 startet erst nach Abschluss von Schritt-1
            return schritt + " erledigt";
        }).subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
// → immer: Schritt-1, Schritt-2, Schritt-3
```

### switchMap – nur das letzte Element zählt

```java
Flux.just("re", "rea", "reac", "react")
    .switchMap(eingabe ->
        Mono.fromCallable(() -> sucheImBackend(eingabe))
            .subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
// → laufende Suchen für "re", "rea", "reac" werden abgebrochen
//   Nur "react" liefert ein Ergebnis
```

**Warum switchMap?** Bei schnell aufeinanderfolgenden Events (Tastatureingaben,
UI-Events) würden ohne `switchMap` veraltete Ergebnisse eintreffen – oder
Ressourcen verschwendet.

---

## flatMapIterable – synchrone Listen expandieren

Wenn die Transformation ein `Iterable` (keine reaktiven Publisher) zurückgibt:

```java
Flux.just(
        List.of("Apfel", "Birne"),
        List.of("Kirsche", "Mango", "Pflaume"),
        List.of("Orange")
)
.flatMapIterable(liste -> liste)
.subscribe(System.out::println);
// → Apfel, Birne, Kirsche, Mango, Pflaume, Orange
```

`flatMapIterable` ist effizienter als `flatMap(Flux::fromIterable)`, da kein
neuer Publisher erzeugt wird.

---

## Vergleich: 03 vs. 04

| | 03_Schedulers | 04_FlatMap |
|---|---|---|
| Hauptthema | Threading & Scheduler | Asynchrone Komposition |
| Parallelismus durch | `subscribeOn` in `flatMap` | `flatMap` selbst |
| Reihenfolge | nicht thematisiert | `concatMap` vs. `flatMap` |
| Abbruch | nicht thematisiert | `switchMap` |
| Synchron expandieren | nicht thematisiert | `flatMapIterable` |

---

## Beispiele in `Main.java`

### Beispiel 1 – map vs. flatMap

```java
Flux.just(1, 2, 3)
    .map(n -> n * 10)                          // → 10, 20, 30
    .subscribe(System.out::println);

Flux.just(1, 2, 3)
    .flatMap(n -> Flux.just(n * 10, n * 10 + 1))  // → 10, 11, 20, 21, 30, 31
    .subscribe(System.out::println);
```

---

### Beispiel 2 – flatMap mit Mono

```java
Flux.just("Alice", "Bob", "Carol")
    .flatMap(name -> ladeBenutzerprofil(name))
    .subscribe(System.out::println);
// → Profil[Alice, Rolle=Admin]
// → Profil[Bob, Rolle=Admin]
// → Profil[Carol, Rolle=Admin]
```

---

### Beispiel 3 – echter Parallelismus

```java
Flux.just("Service-A", "Service-B", "Service-C")
    .flatMap(service ->
        Mono.fromCallable(() -> { Thread.sleep(200); return service + ": OK"; })
            .subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
// Gesamtdauer ≈ 200ms
```

---

### Beispiel 4 – concatMap

```java
Flux.just("Schritt-1", "Schritt-2", "Schritt-3")
    .concatMap(schritt ->
        Mono.fromCallable(() -> { Thread.sleep(100); return schritt + " erledigt"; })
            .subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
// → immer in Reihenfolge: Schritt-1, Schritt-2, Schritt-3
```

---

### Beispiel 5 – switchMap

```java
Flux.just("re", "rea", "reac", "react")
    .switchMap(eingabe ->
        Mono.fromCallable(() -> { Thread.sleep(150); return "Ergebnis für: '" + eingabe + "'"; })
            .subscribeOn(Schedulers.boundedElastic())
    )
    .blockLast();
// → nur das letzte Ergebnis kommt durch
```

---

### Beispiel 6 – flatMapIterable

```java
Flux.just(List.of("Apfel", "Birne"), List.of("Kirsche", "Mango", "Pflaume"), List.of("Orange"))
    .flatMapIterable(liste -> liste)
    .subscribe(System.out::println);
// → 6 einzelne Strings aus 3 Listen
```

---

## Projektstruktur

```
04_FlatMap/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/flatmap/
│   └── Main.java             ← Alle 6 Beispiele
└── OVERVIEW.md
```

---

## Nächste Schritte

Das nächste Modul behandelt Error Handling in reaktiven Pipelines –
`onErrorReturn`, `onErrorResume`, `retry` und `retryWhen`.
