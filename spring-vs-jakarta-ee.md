# Spring vs. Jakarta EE – Eine einleitende Abgrenzung

## Zwei Welten, ein Ziel

Beide Frameworks lösen dasselbe grundlegende Problem: Java-Anwendungen strukturieren, Querschnittsaspekte wie Transaktionen, Security und Persistenz beherrschbar machen. Der Weg dorthin ist jedoch philosophisch sehr verschieden.

---

## Jakarta EE – Standardisierung durch Komitee

Jakarta EE (früher Java EE) ist ein *Spezifikationsstandard*: Ein Gremium (heute die Eclipse Foundation) definiert APIs, und verschiedene Hersteller liefern konforme Implementierungen – WildFly, Payara, GlassFish, Liberty, TomEE. Der Gedanke dahinter: Anwendungscode soll portabel sein, also theoretisch auf jedem zertifizierten Application Server laufen.

Die Konsequenz ist ein eher *deklaratives, annotationsgetriebenes Modell*, das stark auf den Container vertraut – `@Stateless`, `@PersistenceContext`, `@TransactionAttribute`. Der Entwickler beschreibt *was*, der Container erledigt *wie*.

---

## Spring – Pragmatismus aus der Community

Spring entstand 2003 als direkte *Reaktion auf die Schwergewichtigkeit von J2EE*. Rod Johnsons Idee: Dependency Injection und AOP als einfaches POJO-Modell, ohne erzwungene Containerabhängigkeit. Spring ist kein Standard, sondern ein *Produkt* (Pivotal / VMware), das sich durch Pragmatismus und sehr schnelle Innovationszyklen auszeichnet.

Mit Spring Boot wurde dieser Ansatz noch weiter getrieben: Convention over Configuration, eingebetteter Server, opinionated Defaults – alles, damit eine Anwendung so schnell wie möglich läuft.

---

## Der philosophische Kern

| | Jakarta EE | Spring |
|---|---|---|
| Steuerung | Container-zentriert | Anwendungs-zentriert |
| Portabilität | Ziel (spec-basiert) | Kein primäres Ziel |
| Innovation | Konsensprozess (langsam) | Produkt-driven (schnell) |
| Einstieg | Schwergewichtiger | Leichtgewichtiger |

---

## Fazit

In der Praxis hat sich die Grenze über die Jahre verwischt: Spring implementiert viele Jakarta-EE-Spezifikationen (`jakarta.persistence`, `jakarta.transaction`), und Jakarta EE lernt umgekehrt von Spring – CDI ist stark von Springs DI inspiriert. Trotzdem bleibt der kulturelle Unterschied spürbar: Jakarta EE denkt in *Spezifikationen und Zertifizierungen*, Spring denkt in *Developer Experience und Time-to-Market*.
