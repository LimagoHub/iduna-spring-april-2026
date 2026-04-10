# 05_ErrorHandling – Fehlerbehandlung in Project Reactor

## Übersicht

Dieses Modul erklärt, wie Fehler in reaktiven Pipelines behandelt werden.
Reaktive Streams folgen der **Reactive Streams Specification**: Ein Stream endet
entweder mit `onComplete()` oder `onError()` – danach kommt nichts mehr.

---

## Das Grundprinzip: Fehler beenden den Stream

```java
Flux.just(1, 2, 3, 4, 5)
    .map(n -> {
        if (n == 3) throw new RuntimeException("Fehler!");
        return n;
    })
    .subscribe(
        n   -> System.out.println(n),        // 1, 2
        err -> System.out.println("FEHLER"), // stream endet hier
        ()  -> System.out.println("Fertig")  // wird NICHT erreicht
    );
```

Alle Elemente nach dem Fehler gehen verloren. Ohne Fehlerbehandlung landet der
Fehler im `onError`-Handler des Subscribers.

---

## Die Fehlerbehandlungs-Operatoren im Überblick

| Operator | Verhalten | Use-Case |
|---|---|---|
| `onErrorReturn(value)` | Fallback-Wert, Stream endet sauber | Standardwert wenn nichts geht |
| `onErrorResume(fn)` | Fallback-Publisher, Stream läuft weiter | Cache-Fallback, Alternative Quelle |
| `onErrorMap(fn)` | Fehler transformieren, Stream bricht ab | Schichtenarchitektur (SQL → Domain) |
| `doOnError(fn)` | Side-Effect (Logging), Fehler propagiert weiter | Observability, Metriken |
| `retry(n)` | Subscription n-mal wiederholen | Transiente Fehler |
| `retryWhen(...)` | Retry mit Backoff-Strategie | Produktions-Retry gegen überlastete Services |

---

## onErrorReturn – statischer Fallback

```java
Flux.just("Alice", "Bob", null, "Carol")
    .map(name -> name.toUpperCase())   // NullPointerException bei null
    .onErrorReturn("UNBEKANNT")        // Fallback statt Fehler
    .subscribe(System.out::println);
// → ALICE, BOB, UNBEKANNT  (Carol wird nicht mehr geliefert)
```

**Wichtig:** `onErrorReturn` beendet den Stream nach dem Fallback-Wert.
Die restlichen Elemente (hier: "Carol") gehen verloren.

---

## onErrorResume – dynamischer Fallback-Publisher

```java
Flux<String> hauptQuelle = Flux.just("Live A", "Live B")
    .concatWith(Flux.error(new RuntimeException("Verbindung verloren")));

hauptQuelle
    .onErrorResume(err -> Flux.just("[Cache] A", "[Cache] B"))
    .subscribe(System.out::println);
// → Live A, Live B, [Cache] A, [Cache] B
```

Mächtiger als `onErrorReturn`: Der Fallback kann selbst asynchron sein –
z.B. eine Cache-Abfrage, wenn die primäre DB nicht erreichbar ist.

---

## onErrorMap – Fehlertypen transformieren

```java
Mono.fromCallable(() -> { throw new SQLException("DB getrennt"); })
    .onErrorMap(SQLException.class,
        ex -> new RuntimeException("Datenbankfehler: " + ex.getMessage(), ex))
    .subscribe(
        v   -> System.out.println(v),
        err -> System.out.println(err.getMessage())  // "Datenbankfehler: DB getrennt"
    );
```

Wichtig für **Schichtenarchitektur**: Die Service-Schicht soll keine technischen
Exceptions (`SQLException`, `IOException`) nach außen durchreichen.

---

## Fehlerbehandlung in flatMap – Isolation

Ein Fehler in einem Sub-Publisher von `flatMap` propagiert standardmäßig nach
oben und bricht den **gesamten** Stream ab:

```
flatMap("A") → OK
flatMap("B") → FEHLER  ← bricht A und C ebenfalls ab (ohne Isolation)
flatMap("C") → OK
```

Die Lösung: Fehler **innerhalb** des Sub-Publishers behandeln:

```java
Flux.just("Service-A", "Service-B", "Service-C")
    .flatMap(service ->
        rufeDienstAuf(service)
            .onErrorReturn(service + ": FEHLER (isoliert)")  // ← innerhalb des Sub-Publishers
    )
    .subscribe(System.out::println);
// → Service-A: OK, Service-B: FEHLER (isoliert), Service-C: OK
```

Das ist der **häufigste Fehler** bei parallelen API-Calls: Ein fehlschlagender
Call darf nicht alle anderen abbrechen.

---

## retry – automatisches Wiederholen

```java
AtomicInteger versuch = new AtomicInteger(0);

Mono.fromCallable(() -> {
        int v = versuch.incrementAndGet();
        if (v < 3) throw new RuntimeException("Temporärer Fehler");
        return "Erfolg nach " + v + " Versuchen";
    })
    .retry(3)
    .subscribe(System.out::println);
// → Versuch 1... Versuch 2... Versuch 3... Erfolg nach 3 Versuchen
```

`retry()` resubscribed die gesamte Source. Bei Cold Publishers (HTTP-Request,
DB-Query) bedeutet das: die Operation wird erneut ausgeführt.

---

## retryWhen – Backoff-Strategie

In produktiven Anwendungen ist blindes `retry()` gefährlich: Ein überlasteter
Service wird sofort wieder mit Anfragen überflutet.

```java
Mono.fromCallable(() -> { /* potenziell fehlerhafter Call */ })
    .retryWhen(
        Retry.backoff(3, Duration.ofMillis(100))  // max. 3 Versuche, Start: 100ms
             .maxBackoff(Duration.ofMillis(500))  // maximale Pause: 500ms
             .jitter(0.5)                         // ±50% Zufallsstreuung
    )
    .block();
```

```
Versuch 1 → FEHLER → warte ~100ms
Versuch 2 → FEHLER → warte ~200ms
Versuch 3 → FEHLER → warte ~400ms
Versuch 4 → Erfolg (oder endgültiger Fehler)
```

**Jitter** verhindert, dass viele Clients gleichzeitig nach einer Störung
denselben Service treffen ("Thundering Herd").

---

## doOnError – Logging ohne Fehler zu verschlucken

```java
Mono.fromCallable(() -> { throw new RuntimeException("Unerwarteter Fehler"); })
    .doOnError(err -> log.error("Fehler aufgetreten: {}", err.getMessage()))
    .onErrorResume(err -> Mono.just("Fallback-Wert"))
    .subscribe(System.out::println);
// Fehler wird geloggt UND Stream erholt sich
```

`doOnError` ist ein **Side-Effect-Operator**: er reagiert auf den Fehler,
verändert ihn aber nicht. Der Fehler propagiert weiter – erst `onErrorResume`
fängt ihn ab.

Das Standardmuster für **Fehler-Observability**: erst `doOnError` zum Loggen,
dann `onErrorResume` oder `onErrorReturn` zur Behandlung.

---

## Entscheidungsbaum: Welcher Operator?

```
Fehler tritt auf
     │
     ├─ Nur loggen? → doOnError()  (Fehler propagiert weiter)
     │
     ├─ Fehlertyp ändern? → onErrorMap()  (Stream bricht ab)
     │
     ├─ Fallback-Wert (statisch)? → onErrorReturn()
     │
     ├─ Fallback-Publisher (dynamisch)? → onErrorResume()
     │
     └─ Nochmal versuchen?
           ├─ Einfach? → retry(n)
           └─ Mit Backoff? → retryWhen(Retry.backoff(...))
```

---

## Beispiele in `Main.java`

| Beispiel | Thema |
|---|---|
| 1 | Fehler ohne Behandlung – Stream bricht ab |
| 2 | `onErrorReturn` – statischer Fallback-Wert |
| 3 | `onErrorResume` – Fallback auf Cache-Publisher |
| 4 | `onErrorMap` – SQL-Exception → Domänen-Exception |
| 5 | Fehlerbehandlung in `flatMap` – Isolation von Sub-Publishern |
| 6 | `retry` – automatisches Wiederholen bei transienten Fehlern |
| 7 | `retryWhen` – exponentieller Backoff mit Jitter |
| 8 | `doOnError` – Logging + `onErrorResume` kombiniert |

---

## Projektstruktur

```
05_ErrorHandling/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/errorhandling/
│   └── Main.java             ← Alle 8 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 04 vs. 05

| | 04_FlatMap | 05_ErrorHandling |
|---|---|---|
| Fehler in flatMap | erwähnt (Beispiel 3) | vertieft (Isolation, Beispiel 5) |
| Retry | nicht thematisiert | `retry` + `retryWhen` mit Backoff |
| Fallback | nicht thematisiert | `onErrorReturn`, `onErrorResume` |
| Logging | nicht thematisiert | `doOnError` als Side-Effect |

---

## Nächste Schritte

Das nächste Modul erklärt **Hot vs. Cold Publishers** –
einen der häufigsten Stolpersteine beim Einstieg in Reactor:
Wann wird eine Sequenz für jeden Subscriber neu gestartet,
und wann teilen sich alle Subscriber denselben laufenden Stream?
