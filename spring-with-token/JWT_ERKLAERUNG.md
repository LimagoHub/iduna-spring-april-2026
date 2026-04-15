# JWT-Authentifizierung mit Spring Boot

## Inhaltsverzeichnis

1. [Was ist ein Token?](#1-was-ist-ein-token)
2. [Was ist JWT?](#2-was-ist-jwt)
3. [Aufbau eines JWT](#3-aufbau-eines-jwt)
4. [Ablauf der Authentifizierung](#4-ablauf-der-authentifizierung)
5. [Projektstruktur](#5-projektstruktur)
6. [Custom Claims – Wohnort im Token](#6-custom-claims--wohnort-im-token)
7. [Klassenbeschreibungen](#7-klassenbeschreibungen)
8. [Abhängigkeiten (build.gradle.kts)](#8-abhängigkeiten-buildgradlekts)
9. [Konfiguration (application.yml)](#9-konfiguration-applicationyml)
10. [Demo-UI im Browser](#10-demo-ui-im-browser)
11. [Testen mit curl](#11-testen-mit-curl)
12. [Sicherheitshinweise](#12-sicherheitshinweise)

---

## 1. Was ist ein Token?

Ein **Token** ist ein kompaktes, selbst-beschreibendes Berechtigungsobjekt. Statt bei jeder HTTP-Anfrage Benutzername und Passwort mitzusenden, erhält der Client nach einmaliger Anmeldung ein Token, das er bei allen weiteren Anfragen im `Authorization`-Header mitschickt.

### Vergleich: Session vs. Token

| Merkmal            | Session (klassisch)                          | Token (JWT)                              |
|--------------------|----------------------------------------------|------------------------------------------|
| Zustand am Server  | Server speichert Session-Daten               | Server speichert **nichts** (stateless)  |
| Skalierung         | Schwierig (sticky sessions oder Redis nötig) | Einfach (jeder Server kann prüfen)       |
| Gültigkeit         | Servergesteuert (Session-Invalidierung)      | Ablaufdatum im Token selbst kodiert      |
| Eignung            | Web-Apps mit serverseitigem Rendering        | REST-APIs, Microservices, SPAs           |

---

## 2. Was ist JWT?

**JWT** steht für **JSON Web Token** (RFC 7519). Es ist ein offener Standard für die sichere Übertragung von Informationen zwischen zwei Parteien als kompaktes, URL-sicheres JSON-Objekt.

Wichtige Eigenschaften:
- **Kompakt**: Kann als URL-Parameter, POST-Body oder HTTP-Header übertragen werden.
- **Selbst-beschreibend**: Enthält alle notwendigen Informationen direkt im Token.
- **Signiert**: Die Signatur stellt sicher, dass der Inhalt nicht manipuliert wurde.

---

## 3. Aufbau eines JWT

Ein JWT besteht aus drei Base64URL-kodierten Teilen, getrennt durch Punkte:

```
xxxxx.yyyyy.zzzzz
  │      │      └── Signatur
  │      └───────── Payload (Claims)
  └──────────────── Header
```

### 3.1 Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg`: Algorithmus zur Signierung (hier **HS256** = HMAC mit SHA-256)
- `typ`: Token-Typ

### 3.2 Payload (Claims)

```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "iat": 1713100000,
  "exp": 1713103600
}
```

| Claim  | Bedeutung                              |
|--------|----------------------------------------|
| `sub`  | Subject – der Benutzername             |
| `iat`  | Issued At – Ausstellungszeitpunkt      |
| `exp`  | Expiration – Ablaufzeitpunkt           |
| `roles`| Anwendungsspezifischer Claim mit Rollen |

> **Achtung:** Der Payload ist nur Base64URL-kodiert, **nicht verschlüsselt**. Sensitive Daten gehören nicht in den Payload!

### 3.3 Signatur

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

Die Signatur garantiert:
- Der Token wurde von uns ausgestellt (Authentizität)
- Der Inhalt wurde nicht verändert (Integrität)

Der **Secret-Key** muss mindestens 256 Bit (32 Zeichen) lang sein, um den HS256-Algorithmus zu verwenden.

---

## 4. Ablauf der Authentifizierung

```
Client                           Server
  │                                │
  │  POST /auth/login               │
  │  { username, password }  ──────►│
  │                                │ 1. Credentials prüfen
  │                                │ 2. JWT generieren
  │◄────────────────────────────── │
  │  { "token": "eyJ..." }          │
  │                                │
  │  GET /api/hello                 │
  │  Authorization: Bearer eyJ... ─►│
  │                                │ 3. JWT aus Header extrahieren
  │                                │ 4. Signatur prüfen
  │                                │ 5. Ablaufzeit prüfen
  │                                │ 6. Username aus Token lesen
  │                                │ 7. SecurityContext befüllen
  │◄────────────────────────────── │
  │  { "message": "Hallo, admin!" } │
```

### Schritt-für-Schritt

1. **Login**: Client sendet Credentials an `POST /auth/login`
2. **Validierung**: `AuthenticationManager` prüft Benutzername und Passwort
3. **Token-Erstellung**: `JwtService.generateToken()` erstellt ein signiertes JWT
4. **Token-Übertragung**: Client speichert das Token (z.B. im LocalStorage)
5. **Geschützte Anfrage**: Client sendet Token im `Authorization: Bearer <token>`-Header
6. **Filter**: `JwtAuthenticationFilter` läuft bei jeder Anfrage vor den Controllern
7. **Verifikation**: JWT-Signatur und Ablaufzeit werden geprüft
8. **Authentifizierung**: Spring SecurityContext wird mit dem Benutzer befüllt
9. **Autorisierung**: `@PreAuthorize` oder `authorizeHttpRequests` prüfen Berechtigungen

---

## 5. Projektstruktur

```
src/main/java/de/limago/springwithtoken/
├── SpringWithTokenApplication.java    # Einstiegspunkt der Anwendung
│
├── config/
│   └── SecurityConfig.java            # Spring Security Konfiguration
│
├── security/
│   ├── AppUserDetails.java            # Erweitertes UserDetails mit Wohnort
│   ├── JwtAuthenticationFilter.java   # Filter: JWT aus Request lesen
│   ├── JwtService.java                # JWT erstellen, parsen, validieren
│   └── StadtSecurityService.java      # Custom Security Bean für Wohnort-Prüfung
│
└── web/
    ├── AuthController.java            # POST /auth/login
    ├── DemoController.java            # GET /api/hello, GET /api/admin
    ├── LoginRequest.java              # DTO für Login-Daten
    └── StadtController.java           # GET /api/frankfurt (nur Frankfurt)

src/main/resources/
├── application.yml
└── static/
    └── index.html                     # Demo-UI im Browser
```

---

## 6. Custom Claims – Wohnort im Token

### Was sind Custom Claims?

Neben den Standard-Claims (`sub`, `iat`, `exp`) kann der JWT-Payload beliebige eigene Schlüssel-Wert-Paare enthalten. Diese heißen **Custom Claims** und sind der Mechanismus, um anwendungsspezifische Informationen sicher und stateless durch das System zu transportieren.

### Umsetzung in diesem Projekt

Der Wohnort des Benutzers wird als Custom Claim `"wohnort"` ins Token geschrieben und für eine ortsbasierte Zugriffskontrolle verwendet.

**Beteiligte Klassen und ihre Rolle:**

```
SecurityConfig                   AppUserDetails              JwtService
──────────────                   ──────────────              ──────────
admin → "Frankfurt"  ──wrap──►  getWohnort()  ──claim──►  "wohnort": "Frankfurt"
user  → "Berlin"                                            "wohnort": "Berlin"
```

```
JwtAuthenticationFilter          StadtSecurityService        StadtController
───────────────────────          ────────────────────        ───────────────
loadUserByUsername()  ──►  AppUserDetails im Principal  ──►  @PreAuthorize
(liefert AppUserDetails)    getWohnort() = "Frankfurt"?       HTTP 200 / 403
```

### Token-Payload mit Wohnort-Claim

```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "wohnort": "Frankfurt",
  "iat": 1713100000,
  "exp": 1713103600
}
```

### Custom UserDetails: das Decorator-Muster

Spring Security kennt nur das `UserDetails`-Interface. Um Zusatzdaten wie den Wohnort durchzureichen, ohne Spring Security zu verändern, wird `AppUserDetails` als **Wrapper** (Decorator) über das Standard-`User`-Objekt gelegt:

```java
// Standard Spring-User anlegen
UserDetails base = User.withUsername("admin").password(...).roles("ADMIN").build();

// Mit Wohnort wrappen
AppUserDetails admin = new AppUserDetails(base, "Frankfurt");

// Spring Security sieht nur UserDetails – alles funktioniert wie vorher
// Anwendungscode kann zusätzlich admin.getWohnort() aufrufen
```

### Custom Security Bean: @PreAuthorize mit eigenem Bean

Für komplexe Autorisierungsregeln, die nicht mit `hasRole()` oder `hasAuthority()` ausgedrückt werden können, bietet Spring Security das Muster **Custom Security Bean**:

```java
@Service("stadtSecurity")          // Bean-Name
public class StadtSecurityService {
    public boolean istFrankfurt(Authentication auth) {
        if (auth.getPrincipal() instanceof AppUserDetails u) {
            return "Frankfurt".equalsIgnoreCase(u.getWohnort());
        }
        return false;
    }
}
```

Verwendung im Controller:
```java
@PreAuthorize("@stadtSecurity.istFrankfurt(authentication)")
//             ^ Bean-Name     ^ Methode     ^ aktuelles Auth-Objekt (SpEL)
```

Das `@`-Zeichen in SpEL referenziert einen Spring Bean. `authentication` ist ein implizit verfügbares SpEL-Objekt, das auf den `SecurityContext` zugreift.

### Benutzer und Wohnorte in dieser Demo

| Benutzer | Passwort | Rolle       | Wohnort   | `/api/frankfurt` |
|----------|----------|-------------|-----------|-----------------|
| admin    | admin    | ROLE_ADMIN  | Frankfurt | ✓ Zugang        |
| user     | user     | ROLE_USER   | Berlin    | ✗ HTTP 403      |

### Stateless-Prinzip: Wohnort aus dem Token vs. aus der Datenbank

In diesem Projekt wird der Wohnort beim Login aus dem `UserDetailsService` geladen und ins Token geschrieben. Bei jeder Folge-Anfrage lädt der `JwtAuthenticationFilter` die `UserDetails` erneut aus dem `UserDetailsService` – der Wohnort kommt also eigentlich aus dem Speicher, nicht aus dem Token.

In einer echten Microservice-Architektur würde man den Wohnort **direkt aus dem JWT-Claim lesen**, ohne Datenbankzugriff:

```java
// Microservice-Variante: Wohnort aus dem Token lesen
String wohnort = jwtService.getClaimFromToken(token, "wohnort", String.class);
```

Das ist echter **stateless** Betrieb: Der Token enthält alle notwendigen Informationen, kein Service braucht eine Datenbank oder einen zentralen Identity-Store.

---

## 7. Klassenbeschreibungen

### 6.1 `JwtService`

**Paket:** `de.limago.springwithtoken.security`  
**Stereotyp:** `@Service`

Der `JwtService` ist der zentrale Dienst für alle JWT-Operationen. Er kapselt die Bibliothek `jjwt` und stellt drei öffentliche Methoden bereit:

| Methode                             | Beschreibung                                          |
|-------------------------------------|-------------------------------------------------------|
| `generateToken(UserDetails)`        | Erstellt ein signiertes JWT mit Username und Rollen   |
| `getUsernameFromToken(String token)`| Liest den `sub`-Claim (Benutzername) aus dem Token    |
| `validateToken(String token)`       | Prüft Signatur und Ablaufzeit; gibt `true/false` zurück |

**Konfiguration via `application.yml`:**
- `app.jwt.secret` – Symmetrischer HMAC-SHA256-Schlüssel (mind. 32 Zeichen)
- `app.jwt.expiration-ms` – Gültigkeitsdauer in Millisekunden (Standard: 3.600.000 = 1 Stunde)

---

### 6.2 `JwtAuthenticationFilter`

**Paket:** `de.limago.springwithtoken.security`  
**Stereotyp:** `@Component`, erbt von `OncePerRequestFilter`

Dieser Filter wird von Spring Security **bei jeder eingehenden HTTP-Anfrage einmal** ausgeführt, noch bevor der eigentliche Controller erreicht wird. Er ist für die zustandslose Authentifizierung zuständig.

**Ablauf im Filter:**
1. `Authorization`-Header lesen
2. Prüfen ob der Header mit `"Bearer "` beginnt
3. Token extrahieren (Zeichen ab Index 7)
4. Token mit `JwtService.validateToken()` verifizieren
5. Benutzername aus Token lesen
6. `UserDetails` aus dem `UserDetailsService` laden
7. `UsernamePasswordAuthenticationToken` erstellen und in den `SecurityContext` setzen

**Warum `@Lazy` auf `UserDetailsService`?**  
Ohne `@Lazy` würde ein zirkulärer Abhängigkeits-Zyklus entstehen:  
`SecurityConfig` → `UserDetailsService` → `JwtAuthenticationFilter` → `UserDetailsService`  
`@Lazy` verzögert die Instanziierung und bricht den Zyklus.

---

### 6.3 `SecurityConfig`

**Paket:** `de.limago.springwithtoken.config`  
**Annotationen:** `@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`

Die zentrale Spring-Security-Konfiguration. Definiert alle Sicherheitsregeln der Anwendung.

**Beans:**

| Bean                     | Beschreibung                                                  |
|--------------------------|---------------------------------------------------------------|
| `UserDetailsService`     | In-Memory-Benutzerverwaltung mit zwei Testnutzern             |
| `PasswordEncoder`        | BCrypt-Hasher für Passwörter (Standardstärke: 10 Runden)      |
| `AuthenticationManager`  | Koordiniert die Authentifizierung (delegiert an Provider)     |
| `SecurityFilterChain`    | Konfiguriert HTTP-Sicherheit (CSRF, Sessions, Endpunkte)      |

**Wichtige Entscheidungen:**
- **CSRF deaktiviert**: Bei stateless REST-APIs mit JWT ist CSRF-Schutz nicht nötig, da keine Browser-Cookies für die Authentifizierung verwendet werden.
- **Stateless Sessions**: `STATELESS` verhindert, dass Spring Security eine HTTP-Session anlegt.
- `/auth/login` ist öffentlich zugänglich, alle anderen Endpunkte erfordern Authentifizierung.
- Der `JwtAuthenticationFilter` wird **vor** dem Standard-`UsernamePasswordAuthenticationFilter` in die Filter-Kette eingefügt.

---

### 6.4 `AuthController`

**Paket:** `de.limago.springwithtoken.web`  
**Stereotyp:** `@RestController`, Mapping: `/auth`

Stellt den Login-Endpunkt bereit.

| Methode | Endpunkt       | Beschreibung                                           |
|---------|---------------|--------------------------------------------------------|
| POST    | `/auth/login` | Credentials prüfen, JWT generieren, im Body zurückgeben |

**Ablauf:**
1. `AuthenticationManager.authenticate()` wirft eine Exception, wenn die Credentials falsch sind (→ HTTP 401 automatisch durch Spring Security)
2. Bei Erfolg: `UserDetails` laden und `JwtService.generateToken()` aufrufen
3. Token als JSON `{ "token": "eyJ..." }` zurückgeben

---

### 6.5 `DemoController`

**Paket:** `de.limago.springwithtoken.web`  
**Stereotyp:** `@RestController`, Mapping: `/api`

Demonstriert geschützte Endpunkte mit unterschiedlichen Zugriffsrechten.

| Methode | Endpunkt      | Rolle erforderlich | Beschreibung                             |
|---------|--------------|-------------------|------------------------------------------|
| GET     | `/api/hello` | Jeder (authentifiziert) | Gibt personalisierten Gruß zurück |
| GET     | `/api/admin` | `ROLE_ADMIN`      | Nur für Admins zugänglich                |

`@PreAuthorize("hasRole('ADMIN')")` ist **Method-Level Security**: Die Prüfung findet im Anwendungs-Layer statt (nicht im Filter), ermöglicht durch `@EnableMethodSecurity` in `SecurityConfig`.

---

### 7.5 `AppUserDetails`

**Paket:** `de.limago.springwithtoken.security`

Erweitertes `UserDetails`-Objekt, das zusätzlich den **Wohnort** des Benutzers trägt. Implementiert das Decorator-Muster: Das originale Spring-`User`-Objekt wird gewrappt, alle `UserDetails`-Methoden werden delegiert, und `getWohnort()` kommt neu hinzu.

---

### 7.6 `StadtSecurityService`

**Paket:** `de.limago.springwithtoken.security`  
**Stereotyp:** `@Service("stadtSecurity")`

Custom Security Bean für ortsbasierte Autorisierung. Stellt die Methode `istFrankfurt(Authentication)` bereit, die per SpEL in `@PreAuthorize`-Ausdrücken aufgerufen wird. Liest den Wohnort aus dem `AppUserDetails`-Principal des `SecurityContext`.

---

### 7.7 `StadtController`

**Paket:** `de.limago.springwithtoken.web`  
**Stereotyp:** `@RestController`, Mapping: `/api`

| Methode | Endpunkt         | Bedingung               | Beschreibung                          |
|---------|-----------------|-------------------------|---------------------------------------|
| GET     | `/api/frankfurt` | Wohnort = "Frankfurt"  | Nur für Frankfurter Benutzer          |

---

### 7.8 `LoginRequest`

**Paket:** `de.limago.springwithtoken.web`

Ein Java **Record** – ein unveränderliches Datentransferobjekt (DTO) für die Login-Anfrage.

```java
public record LoginRequest(String username, String password) {}
```

Records sind seit Java 16 verfügbar und generieren automatisch:
- Konstruktor, Getter (`username()`, `password()`), `equals()`, `hashCode()`, `toString()`

---

## 7. Abhängigkeiten (build.gradle.kts)

| Abhängigkeit                                     | Zweck                                         |
|--------------------------------------------------|-----------------------------------------------|
| `spring-boot-starter-web`                        | REST-Controller, embedded Tomcat              |
| `spring-boot-starter-security`                   | Spring Security Framework                     |
| `io.jsonwebtoken:jjwt-api:0.12.6`               | jjwt API (zum Kompilieren)                    |
| `io.jsonwebtoken:jjwt-impl:0.12.6` (runtime)    | jjwt Implementierung                          |
| `io.jsonwebtoken:jjwt-jackson:0.12.6` (runtime) | JSON-Serialisierung für jjwt via Jackson      |
| `spring-boot-starter-test`                       | JUnit 5, Mockito, AssertJ                     |
| `spring-security-test`                           | Security-Test-Hilfsmethoden                   |

---

## 8. Konfiguration (application.yml)

```yaml
server:
  port: 8080

app:
  jwt:
    secret: "mein-geheimer-jwt-schluessel-fuer-hmac-sha256-mindestens-32-zeichen"
    expiration-ms: 3600000   # 1 Stunde
```

> **Produktions-Hinweis**: Der `secret`-Wert darf **niemals** im Klartext im Repository liegen. In Produktionsumgebungen werden Umgebungsvariablen oder Secrets-Manager (z.B. HashiCorp Vault, AWS Secrets Manager) verwendet:
> ```yaml
> app:
>   jwt:
>     secret: ${JWT_SECRET}
> ```

---

## 9. Demo-UI im Browser

### Warum kann der Browser keine JWT-geschützten Endpunkte direkt aufrufen?

Ein Browser schickt bei der Eingabe einer URL in die Adressleiste einen einfachen `GET`-Request – ohne die Möglichkeit, zusätzliche HTTP-Header wie `Authorization: Bearer <token>` mitzugeben. Außerdem kann der Browser kein Login-Formular mit JSON-Body an einen REST-Endpunkt senden.

### Lösung: Statische HTML-Seite

Spring Boot liefert Dateien aus `src/main/resources/static/` automatisch als statische Ressourcen aus – ohne Controller, ohne Authentifizierung. Die Datei `index.html` dort ist unter `http://localhost:8080/` erreichbar.

```
src/main/resources/
├── application.yml
└── static/
    └── index.html     ←  wird von Spring Boot direkt ausgeliefert
```

Die Seite nutzt die Browser-eigene **Fetch API** (JavaScript), um:
1. Login-Request mit JSON-Body abzusetzen
2. Das erhaltene Token im Arbeitsspeicher der Seite zu halten
3. Folge-Requests mit dem `Authorization`-Header zu verschicken

### Anpassung in SecurityConfig

Da alle Endpunkte außer `/auth/login` standardmäßig gesichert sind, musste die Freigabeliste erweitert werden:

```java
.requestMatchers("/auth/login", "/", "/index.html").permitAll()
```

Ohne diese Freigabe würde Spring Security den Zugriff auf die HTML-Seite mit HTTP 401 ablehnen, bevor überhaupt JavaScript ausgeführt werden kann.

### Funktionsumfang der Demo-Seite

| Bereich | Beschreibung |
|---------|-------------|
| **Login** | Formular für Benutzername/Passwort, sendet `POST /auth/login`, zeigt das erhaltene Token an |
| **Status-Badge** | Zeigt an, ob ein Token vorhanden ist und für welchen Benutzer |
| **`/api/hello`** | Geschützter Endpunkt – funktioniert für alle authentifizierten Benutzer |
| **`/api/admin`** | Admin-Endpunkt – funktioniert nur mit `admin`, bei `user` kommt HTTP 403 |
| **Benutzer wechseln** | Schnell-Button „Als user" befüllt das Formular für den zweiten Testnutzer |
| **Logout** | Löscht das Token aus dem Seitenspeicher |

### Token-Lebenszyklus im Browser

```
Seite laden  →  Login  →  Token im JS-Speicher  →  API-Aufrufe  →  Logout / Tab schließen
                                                                          ↓
                                                               Token wird verworfen
                                                          (kein Cookie, kein LocalStorage)
```

Das Token wird bewusst **nur im JavaScript-Arbeitsspeicher** gehalten (Variable `currentToken`). Nach einem Seiten-Reload ist es weg und ein erneuter Login ist nötig. In echten Anwendungen wird das Token oft im `localStorage` oder `sessionStorage` gespeichert – mit entsprechenden Sicherheitsüberlegungen (XSS-Risiko).

### Starten und aufrufen

```bash
# App starten
./gradlew bootRun

# Demo-Seite im Browser öffnen
http://localhost:8080/
```

---

## 10. Testen mit curl

### Login (Token erhalten)

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq .
```

Antwort:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXX0.xxx"
}
```

### Geschützter Endpunkt aufrufen

```bash
TOKEN="eyJ..."

curl -s http://localhost:8080/api/hello \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Admin-Endpunkt (nur mit ROLE_ADMIN)

```bash
curl -s http://localhost:8080/api/admin \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Token dekodieren (zur Analyse)

```bash
# Payload anzeigen (zweiter Teil, Base64URL-dekodiert)
echo "eyJzdWIi..." | base64 -d
```

Oder online: [jwt.io](https://jwt.io)

---

## 11. Sicherheitshinweise

| Thema | Empfehlung |
|-------|-----------|
| Secret-Key | Mindestens 256 Bit, zufällig generiert, niemals im Code |
| HTTPS | JWT immer nur über HTTPS übertragen (Base64 ist kein Schutz!) |
| Ablaufzeit | Kurz halten (z.B. 15 min) + Refresh-Token-Mechanismus |
| Payload | Keine sensitiven Daten (Passwörter, SSN etc.) im Payload |
| Token-Invalidierung | JWTs sind stateless – für Logout Blocklist oder kurze Laufzeit nötig |
| Algorithmus | HS256 für symmetrisch (ein Service), RS256 für asymmetrisch (mehrere Services) |

---

## 12. OAuth2 – ein kurzer Überblick und Vergleich

### Was ist OAuth2?

**OAuth2** (RFC 6749) ist ein **Autorisierungsframework**, kein Authentifizierungsprotokoll. Es löst ein spezifisches Problem: Wie kann eine Anwendung im Auftrag eines Benutzers auf Ressourcen einer **anderen** Anwendung zugreifen – ohne dabei das Passwort des Benutzers zu kennen?

Das klassische Beispiel: Eine App möchte auf deine Google-Kontakte zugreifen. Mit OAuth2 erlaubst du das, ohne der App dein Google-Passwort zu geben.

### Die vier Rollen in OAuth2

| Rolle | Bedeutung | Beispiel |
|-------|-----------|---------|
| **Resource Owner** | Der Benutzer, dem die Daten gehören | Du selbst |
| **Client** | Die Anwendung, die Zugriff will | Eine Drittanbieter-App |
| **Authorization Server** | Stellt Tokens aus (nach Login des Benutzers) | Google, Keycloak, Okta |
| **Resource Server** | Besitzt die geschützten Daten | Google Contacts API |

### Der OAuth2-Ablauf (Authorization Code Flow)

```
Benutzer          Client-App           Authorization Server      Resource Server
   │                  │                        │                       │
   │  Klick "Login    │                        │                       │
   │  mit Google"     │                        │                       │
   │─────────────────►│                        │                       │
   │                  │  Redirect zu Google    │                       │
   │                  │───────────────────────►│                       │
   │  Login + Zustimmung                       │                       │
   │──────────────────────────────────────────►│                       │
   │                  │  Authorization Code    │                       │
   │                  │◄───────────────────────│                       │
   │                  │  Code gegen Token tauschen                     │
   │                  │───────────────────────►│                       │
   │                  │  Access Token (JWT)    │                       │
   │                  │◄───────────────────────│                       │
   │                  │  API-Aufruf mit Token  │                       │
   │                  │───────────────────────────────────────────────►│
   │                  │  Geschützte Ressource  │                       │
   │                  │◄───────────────────────────────────────────────│
```

### OpenID Connect (OIDC) – OAuth2 + Authentifizierung

OAuth2 allein authentifiziert den Benutzer nicht – es autorisiert nur den Zugriff auf Ressourcen. **OpenID Connect (OIDC)** ist eine dünne Schicht auf OAuth2, die Authentifizierung ergänzt: Neben dem Access Token wird ein **ID Token** ausgestellt, das die Identität des Benutzers beschreibt (wer er ist, nicht nur was er darf).

```
OAuth2  →  "Du darfst auf diese Ressource zugreifen"
OIDC    →  "Du bist eingeloggt als user@example.com"
```

### Unser Verfahren vs. OAuth2 – der Unterschied

| Merkmal | Unser Projekt (Custom JWT) | OAuth2 / OIDC |
|---------|---------------------------|---------------|
| **Zweck** | Authentifizierung in einer eigenen Anwendung | Delegierte Autorisierung über Systemgrenzen hinweg |
| **Wer stellt Tokens aus?** | Unsere eigene Anwendung (`JwtService`) | Ein externer Authorization Server (Keycloak, Google, Okta) |
| **Benutzeridentität** | In unserer eigenen Datenbank/Memory | Beim externen Identity Provider |
| **Login-Dialog** | Unser eigenes Formular | Login-Seite des Authorization Servers |
| **Typischer Einsatz** | Eine einzelne Anwendung, eigene Benutzer | Mehrere Anwendungen, SSO, Drittanbieter-Integration |
| **Komplexität** | Gering – alles selbst implementiert | Höher – aber battle-tested Bibliotheken vorhanden |
| **Standard** | Proprietär (wir nutzen JWT als Format) | Offener Standard, breit unterstützt |

### Fazit

In unserem Projekt sind wir **selbst der Authorization Server**: Wir prüfen Credentials, stellen JWTs aus und validieren sie. Das ist für eine eigenständige Anwendung mit eigener Benutzerverwaltung vollkommen richtig und ausreichend.

OAuth2 + OIDC käme dann ins Spiel, wenn:
- Benutzer sich mit einem bestehenden Account anmelden sollen ("Login mit Google/GitHub/Microsoft")
- Mehrere Anwendungen dieselbe Benutzeridentität teilen sollen (**Single Sign-On**)
- Eine Anwendung im Auftrag des Benutzers auf Dienste Dritter zugreifen soll

In Spring Boot würde man OAuth2 über den Starter `spring-boot-starter-oauth2-resource-server` (für die Resource-Server-Seite) oder `spring-boot-starter-oauth2-client` (für den Client) einbinden – und die gesamte Filter-Kette, die wir hier selbst gebaut haben, wird dann durch die Bibliothek übernommen.
