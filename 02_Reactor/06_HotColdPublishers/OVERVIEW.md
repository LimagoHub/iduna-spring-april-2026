# 06_HotColdPublishers – Hot vs. Cold in Project Reactor

## Übersicht

Dieses Modul erklärt einen der häufigsten Stolpersteine beim Einstieg in Reactor:
den Unterschied zwischen **Cold** und **Hot Publishers**.

> **Cold Publisher:** Startet die Produktion neu für jeden Subscriber.
> **Hot Publisher:** Produziert unabhängig von Subscribern; wer zu spät kommt, verpasst Elemente.

---

## Die Analogie

| | Cold Publisher | Hot Publisher |
|---|---|---|
| Analogie | DVD | Radiosender |
| Start | Bei jeder Subscription | Läuft unabhängig |
| Verpasste Elemente | Keine – jeder sieht alles | Möglich – wer zu spät kommt, verpasst |
| Beispiele | `Flux.just()`, `Flux.range()`, HTTP-Call | `Flux.interval()`, WebSocket, UI-Events |

---

## Cold Publisher – das Standardverhalten

Alle gängigen Reactor-Quellen sind kalt:

```java
Flux<Integer> cold = Flux.range(1, 3)
    .map(n -> { System.out.println("produziere: " + n); return n; });

cold.subscribe(n -> System.out.println("Sub1: " + n));
cold.subscribe(n -> System.out.println("Sub2: " + n));

// "produziere" erscheint 6× – für jeden Subscriber neu
```

**Wichtige Konsequenz:** Ein HTTP-Call oder eine DB-Abfrage als Cold Publisher
wird für jeden Subscriber **erneut** ausgeführt. Das ist oft gewollt – aber
nicht immer.

---

## Cold → Hot: publish() + connect()

`publish()` erzeugt einen `ConnectableFlux`. Die Produktion startet erst beim
Aufruf von `connect()` – so können sich mehrere Subscriber vorab registrieren:

```java
ConnectableFlux<Integer> connectable = Flux.range(1, 4)
    .map(n -> { System.out.println("produziere: " + n); return n; })
    .publish();

connectable.subscribe(n -> System.out.println("Sub1: " + n));
connectable.subscribe(n -> System.out.println("Sub2: " + n));

connectable.connect();  // Produktion startet jetzt
// "produziere" erscheint nur 4× – geteilt zwischen Sub1 und Sub2
```

```
Ohne publish():  Sub1 → 4×produzieren, Sub2 → 4×produzieren  = 8× gesamt
Mit publish():   connect() → 4×produzieren, beide erhalten alle Elemente
```

---

## autoConnect() – automatisch verbinden

`autoConnect(n)` startet die Verbindung automatisch, sobald `n` Subscriber
vorhanden sind. Kein manuelles `connect()` nötig:

```java
Flux<Integer> autoHot = Flux.range(1, 4)
    .publish()
    .autoConnect(2);  // startet bei 2 Subscribern

autoHot.subscribe(n -> System.out.println("Sub1: " + n));  // wartet
autoHot.subscribe(n -> System.out.println("Sub2: " + n));  // startet jetzt
```

| `autoConnect(n)` | Verhalten |
|---|---|
| `autoConnect(1)` | Startet beim ersten Subscriber |
| `autoConnect(2)` | Wartet auf 2 Subscriber, dann Start |

---

## share() – refCount-basiertes Multicast

`share()` ist eine Kurzform für `publish().refCount()`:

- Startet automatisch beim ersten Subscriber
- Beendet die Upstream-Subscription, wenn der letzte Subscriber geht
- Startet neu (Cold-Restart), wenn wieder ein neuer Subscriber kommt

```java
Flux<Integer> shared = Flux.range(1, 4)
    .map(n -> { System.out.println("produziere: " + n); return n; })
    .share();

shared.subscribe(n -> System.out.println("Sub1: " + n));
shared.subscribe(n -> System.out.println("Sub2: " + n));
// "produziere" erscheint 4× – nicht 8×
```

**Wann verwenden?** Wenn mehrere Teile der Anwendung denselben teuren Datenstrom
(HTTP-Poll, WebSocket) abonnieren, ohne die Quelle mehrfach zu starten.

---

## cache() – Replay für späte Subscriber

`cache()` speichert emittierte Elemente und spielt sie jedem neuen Subscriber
sofort ab – auch wenn der Stream bereits beendet ist:

```java
Flux<Integer> cached = Flux.range(1, 3)
    .map(n -> { System.out.println("produziere (einmalig): " + n); return n; })
    .cache();

cached.subscribe(n -> System.out.println("Sub1: " + n));
// Stream ist jetzt beendet

cached.subscribe(n -> System.out.println("Sub2: " + n));
// "produziere" erscheint nur 3× – Sub2 bekommt Cache-Replay
```

### share() vs. cache()

| | `share()` | `cache()` |
|---|---|---|
| Replay | Nein | Ja |
| Späte Subscriber | Verpassen Elemente | Erhalten Replay |
| Stream beendet? | Neustart bei nächstem Sub | Replay aus Cache |
| Use-Case | Live-Stream (WebSocket) | Einmalige Initialisierung (HTTP-Config) |

---

## Sinks – programmatisch emittieren

`Sinks` ermöglichen es, von imperativem Code aus in einen reaktiven Stream zu
emittieren – das Bindeglied zwischen der Nicht-Reactor-Welt und Reactor:

```java
Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
Flux<String> stream = sink.asFlux();

stream.subscribe(s -> System.out.println("Sub1: " + s));
stream.subscribe(s -> System.out.println("Sub2: " + s));

sink.tryEmitNext("Hallo");   // beide Subscriber erhalten "Hallo"
sink.tryEmitNext("Welt");    // beide Subscriber erhalten "Welt"
sink.tryEmitComplete();
```

**Typische Anwendungsfälle:**
- UI-Events oder WebSocket-Nachrichten in einen Flux umwandeln
- In einem Callback (z.B. Listener) in einen Stream emittieren
- Tests: manuell Elemente oder Fehler in eine Pipeline einspeisen

---

## Entscheidungsbaum: Welcher Operator?

```
Mehrere Subscriber sollen denselben Stream teilen?
     │
     ├─ Alle müssen alle Elemente sehen (inkl. vergangene)?
     │       └─ cache()
     │
     ├─ Start erst wenn alle bereit sind?
     │       └─ publish() + connect()  oder  publish().autoConnect(n)
     │
     ├─ Teuer, aber Replay nicht nötig?
     │       └─ share()
     │
     └─ Von außen in Stream emittieren?
             └─ Sinks
```

---

## Beispiele in `Main.java`

| Beispiel | Thema |
|---|---|
| 1 | Cold Publisher – Produktion für jeden Subscriber neu |
| 2 | Hot Publisher – `Flux.interval` als natürlicher Hot Source |
| 3 | `publish()` + `connect()` – Cold in Hot umwandeln |
| 4 | `autoConnect()` – automatisch verbinden bei n Subscribern |
| 5 | `share()` – `publish().refCount()` Kurzform |
| 6 | `cache()` – Replay für späte Subscriber |
| 7 | `Sinks` – programmatisch in Stream emittieren |

---

## Projektstruktur

```
06_HotColdPublishers/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── src/main/java/de/hotcold/
│   └── Main.java             ← Alle 7 Beispiele
└── OVERVIEW.md
```

---

## Vergleich: 05 vs. 06

| | 05_ErrorHandling | 06_HotColdPublishers |
|---|---|---|
| Kernfrage | Was passiert wenn es schiefgeht? | Wer bekommt welche Elemente? |
| Subscriber-Verhalten | nicht thematisiert | zentral |
| Operator-Fokus | onError*, retry | publish, share, cache, Sinks |
| Threading | Backoff auf boundedElastic | nicht Hauptthema |

---

## Nächste Schritte

Das nächste Modul deckt **Stream-Kombination** ab: Wie mehrere Publisher
zusammengeführt werden – `merge()`, `concat()`, `zip()`, `combineLatest()`.
