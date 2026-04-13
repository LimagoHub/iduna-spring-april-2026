# Aggregat in Domain-Driven Design

Ein **Aggregat** ist eine Gruppe eng zusammengehöriger Objekte, die als geschlossene Einheit behandelt wird. Eine Entität übernimmt dabei die Rolle des **Aggregate Root** – sie ist der einzige Einstiegspunkt für alle Operationen von außen und stellt sicher, dass Geschäftsregeln und Datenkonsistenz innerhalb des Aggregates gewahrt bleiben.

## Beispiel

Das klassische Beispiel: Eine `Bestellanforderung` enthält mehrere `Bestellpositionen`. Eine `Bestellposition` hat außerhalb einer `Bestellanforderung` keinen eigenständigen Wert – sie ist ein untergeordnetes Objekt, das nur im Kontext des Ganzen Bedeutung hat. Deshalb gehören beide zusammen in ein Aggregat, mit `Bestellanforderung` als Root.

## Warum ist das sinnvoll?

Ohne Aggregat kann jeder Code direkt auf eine `Bestellposition` zugreifen und sie verändern – auch wenn die Bestellanforderung längst genehmigt ist. Mit einem Aggregat wird das verhindert: Alle Operationen auf `Bestellposition` laufen zwingend über `bestellanforderung.bestellpositionAendern()`, und gespeichert wird immer das gesamte Aggregat über sein Repository. So lassen sich Invarianten wie „nach Genehmigung keine Änderungen mehr" zuverlässig durchsetzen, ohne dass diese Prüfung an zehn verschiedenen Stellen im Code verstreut sein muss.

## Fazit

Ein Aggregat zieht eine **Konsistenzgrenze** um zusammengehörige Objekte und erzwingt, dass Geschäftslogik nicht umgangen werden kann.

---

*Basierend auf: [An In-Depth Understanding of Aggregation in Domain-Driven Design](https://www.alibabacloud.com/blog/an-in-depth-understanding-of-aggregation-in-domain-driven-design_598034)*
