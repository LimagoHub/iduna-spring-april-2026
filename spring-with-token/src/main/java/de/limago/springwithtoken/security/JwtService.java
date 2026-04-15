package de.limago.springwithtoken.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Dienst für alle JWT-Operationen (Erstellen, Lesen, Validieren).
 *
 * <p>Ein JWT (JSON Web Token) besteht aus drei Base64URL-kodierten Teilen:
 * <pre>
 *   Header.Payload.Signatur
 * </pre>
 * Dieser Service verwendet den symmetrischen Algorithmus <b>HS256</b> (HMAC-SHA256).
 * Das bedeutet: Derselbe geheime Schlüssel wird zum Signieren <i>und</i> zum Verifizieren
 * verwendet – geeignet für Anwendungen mit nur einem Backend-Service.
 *
 * <p>Konfiguration in {@code application.yml}:
 * <ul>
 *   <li>{@code app.jwt.secret} – symmetrischer Schlüssel, mind. 32 Zeichen (256 Bit)</li>
 *   <li>{@code app.jwt.expiration-ms} – Gültigkeitsdauer in ms (Standard: 3 600 000 = 1 Stunde)</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 */
@Service
public class JwtService {

    /**
     * Der vorberechnete SecretKey-Objekt für HMAC-SHA256.
     * Wird einmal im Konstruktor aus dem konfigurierten String erzeugt
     * und für alle Token-Operationen wiederverwendet.
     */
    private final SecretKey secretKey;

    /**
     * Gültigkeitsdauer eines Tokens in Millisekunden.
     * Wird aus der Konfiguration gelesen; Standardwert: 3 600 000 ms (= 1 Stunde).
     */
    private final long expirationMs;

    /**
     * Konstruktor – Spring injiziert die Werte aus {@code application.yml}.
     *
     * @param secret       Der geheime Zeichenketten-Schlüssel (mind. 32 Zeichen für HS256)
     * @param expirationMs Ablaufzeit in Millisekunden
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {

        // Keys.hmacShaKeyFor() konvertiert den Byte-Array in ein javax.crypto.SecretKey-Objekt.
        // StandardCharsets.UTF_8 stellt sicher, dass die Kodierung plattformunabhängig ist.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Erstellt ein signiertes JWT für den angegebenen Benutzer.
     *
     * <p>Das Token enthält folgende Claims:
     * <ul>
     *   <li>{@code sub} (Subject) – der Benutzername</li>
     *   <li>{@code roles} – Liste der Rollen/Authorities als Strings, z.B. ["ROLE_ADMIN"]</li>
     *   <li>{@code iat} (Issued At) – aktueller Zeitstempel</li>
     *   <li>{@code exp} (Expiration) – Ablaufzeitpunkt = jetzt + expirationMs</li>
     * </ul>
     *
     * @param userDetails Die Spring-Security-Benutzerdetails (Username + Authorities)
     * @return Das kompakte, signierte JWT als String (Format: "xxxxx.yyyyy.zzzzz")
     */
    public String generateToken(UserDetails userDetails) {

        // Alle GrantedAuthority-Objekte in eine Liste von Strings umwandeln
        // (z.B. "ROLE_ADMIN", "ROLE_USER")
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var builder = Jwts.builder()
                // sub-Claim: identifiziert den Benutzer eindeutig
                .subject(userDetails.getUsername())
                // Anwendungsspezifischer Claim "roles"
                .claim("roles", roles);

        // Custom Claim "wohnort": nur hinzufügen, wenn AppUserDetails vorliegt.
        // So bleibt JwtService unabhängig von der konkreten UserDetails-Implementierung –
        // er "erkennt" AppUserDetails, muss aber nicht davon abhängen.
        if (userDetails instanceof AppUserDetails appUser) {
            builder.claim("wohnort", appUser.getWohnort());
        }

        return builder
                // iat-Claim: Zeitpunkt der Token-Erstellung
                .issuedAt(new Date())
                // exp-Claim: Token läuft nach expirationMs ab
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                // Token mit HMAC-SHA256 signieren – verhindert Manipulation
                .signWith(secretKey)
                // Alle Teile kodieren, kombinieren und als kompakten String zurückgeben
                .compact();
    }

    /**
     * Liest den Benutzernamen aus einem (bereits validierten) Token.
     *
     * <p>Der Benutzername steckt im {@code sub}-Claim (Subject) des JWT-Payloads.
     *
     * @param token Das JWT als String
     * @return Den Benutzernamen (Subject-Claim)
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Liest den {@code wohnort}-Claim aus dem Token.
     *
     * @param token Das JWT als String
     * @return Den Wohnort-String oder {@code null}, wenn der Claim fehlt
     */
    public String getWohnortFromToken(String token) {
        return parseClaims(token).get("wohnort", String.class);
    }

    /**
     * Prüft, ob ein Token gültig ist (korrekte Signatur + nicht abgelaufen).
     *
     * <p>Die Validierung erfolgt implizit durch {@link #parseClaims(String)}:
     * <ul>
     *   <li>Bei falscher Signatur wirft jjwt eine {@code SignatureException}</li>
     *   <li>Bei abgelaufenem Token wirft jjwt eine {@code ExpiredJwtException}</li>
     *   <li>Bei fehlerhaftem Format eine {@code MalformedJwtException}</li>
     * </ul>
     *
     * @param token Das JWT als String
     * @return {@code true} wenn das Token gültig ist, {@code false} bei jeglichem Fehler
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            // Alle JWT-Ausnahmen (abgelaufen, falsche Signatur, fehlerhaftes Format)
            // werden hier abgefangen und als "ungültig" behandelt.
            // Keine detaillierte Fehlerausgabe, um keine Hinweise für Angreifer zu liefern.
            return false;
        }
    }

    /**
     * Parst das JWT und gibt den Payload (Claims) zurück.
     *
     * <p>Der Parser verifiziert dabei automatisch:
     * <ul>
     *   <li>Die HMAC-SHA256-Signatur mit dem gespeicherten {@code secretKey}</li>
     *   <li>Das Ablaufdatum ({@code exp}-Claim)</li>
     * </ul>
     *
     * @param token Das JWT als String
     * @return Die geparsten Claims (Payload des Tokens)
     * @throws io.jsonwebtoken.JwtException bei ungültigem Token
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                // Signaturprüfung: Der Parser verwendet denselben secretKey wie beim Signieren
                .verifyWith(secretKey)
                .build()
                // Parst das Token und wirft eine Exception, wenn Signatur oder Ablaufzeit ungültig
                .parseSignedClaims(token)
                // getPayload() gibt den eigentlichen Claims-Teil (Payload) zurück
                .getPayload();
    }
}
