# Modul 11: Context

## Lernziele

- Verstehen, warum ein Context in reaktiven Systemen nötig ist
- `contextWrite()` zum Einschreiben von Werten verwenden
- `Mono.deferContextual()` und `Flux.deferContextual()` zum Lesen einsetzen
- Die Ausbreitungsrichtung des Contexts verstehen (entgegen dem Datenfluss)
- Mehrere Context-Einträge schichten und aktualisieren

---

## Warum Context?

In klassischem Java nutzt man `ThreadLocal` für Querschnittsbelange wie
Korrelations-IDs, Benutzer-Sessions oder Locale-Einstellungen. In reaktiven
Systemen wechselt ein Request jedoch ständig zwischen Threads – `ThreadLocal`
funktioniert daher **nicht**.

```
Klassisch (funktioniert nicht reaktiv):
  ThreadLocal<String> reqId = new ThreadLocal<>();
  reqId.set("REQ-001");
  // Nach publishOn() läuft der Code auf einem anderen Thread!

Reaktiv (korrekt):
  Flux.just(...)
      .flatMap(x -> Mono.deferContextual(ctx -> {
          String reqId = ctx.get("requestId");
          // reqId ist immer verfügbar, egal auf welchem Thread
          ...
      }))
      .contextWrite(Context.of("requestId", "REQ-001"))
```

---

## Ausbreitungsrichtung

Der Context fließt **entgegen dem Datenstrom** – also von unten nach oben,
von der Subscription zur Quelle.

```
  Quelle  →  map()  →  flatMap()  →  Subscriber
     ↑           ↑         ↑
     └───────────┴─────────┘
         Context fließt AUFWÄRTS
```

**Konsequenz:** `contextWrite()` muss **unterhalb** der Stelle stehen,
an der der Context gelesen wird (in der Code-Kette).

```java
// RICHTIG:
Mono.deferContextual(ctx -> Mono.just(ctx.get("x")))
    .contextWrite(Context.of("x", "Wert"))   // unterhalb → korrekt
    .subscribe(...);

// FALSCH (x ist nicht sichtbar):
Mono.deferContextual(ctx -> Mono.just(ctx.getOrDefault("x", "fehlt")))
    .map(s -> s + "!")
    .contextWrite(Context.of("x", "Wert"))   // nur für map() sichtbar, nicht oben
    .subscribe(...);
```

---

## Wichtige API-Methoden

### Context schreiben

```java
.contextWrite(Context.of("key", value))               // neuer Context
.contextWrite(ctx -> ctx.put("key", value))            // bestehenden erweitern
.contextWrite(ctx -> ctx.putAll(anderer))              // mehrere mergen
```

### Context lesen

```java
Mono.deferContextual(ctx -> {
    String wert = ctx.get("key");                      // Exception wenn fehlt
    String wert = ctx.getOrDefault("key", "default"); // mit Fallback
    Optional<String> opt = ctx.getOrEmpty("key");     // als Optional
    boolean hat = ctx.hasKey("key");                   // Existenz prüfen
    return Mono.just(wert);
});
```

### Context.of() – Mehrere Einträge

```java
Context.of("k1", v1, "k2", v2)                       // bis 5 Paare direkt
Context.of("k1", v1).put("k2", v2)                   // alternativ
```

---

## Schichtung von contextWrite()

Wenn mehrere `contextWrite()`-Aufrufe denselben Schlüssel setzen,
gewinnt der **weiter unten** (näher am Subscriber) stehende Aufruf –
er ist näher an der Quelle und wird zuerst verarbeitet.

```java
Mono.deferContextual(ctx -> Mono.just(ctx.get("user")))
    .contextWrite(Context.of("user", "Outer"))  // weiter oben → verliert
    .contextWrite(Context.of("user", "Inner"))  // weiter unten → gewinnt
    .subscribe(System.out::println);  // → "Inner"
```

---

## Beispiele in diesem Modul

| Beispiel | Thema                            | Neue APIs                                             |
|----------|----------------------------------|-------------------------------------------------------|
| 1        | Context schreiben und lesen      | `contextWrite()`, `deferContextual()`, `getOrDefault()` |
| 2        | Context in Flux-Pipelines        | `flatMap` + `deferContextual()`                       |
| 3        | Ausbreitungsrichtung             | Korrekte vs. falsche Positionierung von contextWrite  |
| 4        | Korrelations-ID im flatMap       | Praxismuster: Request-ID durch Pipeline leiten        |
| 5        | Mehrere Einträge, Schichtung     | `Context.of(k1,v1,k2,v2)`, Überschreib-Regel         |
| 6        | Context aktualisieren            | `ctx.put()` erzeugt neuen (immutablen) Context        |

---

## Projektstruktur

```
11_Context/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── src/
    └── main/
        └── java/
            └── de/context/
                └── Main.java
```

**Starten:**
```bash
./gradlew run
```

---

## Vergleich: 10_Sinks → 11_Context

| Aspekt            | 10_Sinks                          | 11_Context                           |
|-------------------|-----------------------------------|--------------------------------------|
| Thema             | Imperative Elemente einspeisen    | Querschnittsdaten transportieren     |
| Richtung          | Datenstrom (Source → Subscriber)  | Entgegen dem Datenstrom              |
| Analogie          | Queue / Kanal                     | ThreadLocal für reaktive Systeme     |
| Anwendungsfall    | WebSocket, Callbacks              | Request-ID, Auth-Token, Locale       |

---

## Nächste Schritte

→ **12_Debugging**: Wie findet man heraus, was in einer reaktiven Pipeline
  schiefläuft? Log, Checkpoint, Hooks und reaktive Stack-Traces.
