package de.limago.springwithtoken.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo-Controller mit geschützten Endpunkten zur Demonstration der Autorisierung.
 *
 * <p>Zeigt zwei unterschiedliche Schutzmechanismen:
 * <ol>
 *   <li><b>Globale Absicherung</b> via {@code SecurityConfig.authorizeHttpRequests()}:
 *       Alle {@code /api/**}-Endpunkte erfordern Authentifizierung (gültiges JWT)</li>
 *   <li><b>Method-Level Security</b> via {@code @PreAuthorize}:
 *       Feingranulare Rollenprüfung direkt auf der Methode</li>
 * </ol>
 *
 * <h3>Voraussetzung für Method-Level Security:</h3>
 * {@code @EnableMethodSecurity} muss in der {@link de.limago.springwithtoken.config.SecurityConfig}
 * aktiviert sein.
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /**
     * Öffentlicher (aber authentifizierter) Endpunkt – zugänglich für alle angemeldeten Benutzer.
     *
     * <p>Spring Security injiziert das {@link Authentication}-Objekt automatisch aus dem
     * {@link org.springframework.security.core.context.SecurityContext}, der zuvor vom
     * {@link de.limago.springwithtoken.security.JwtAuthenticationFilter} befüllt wurde.
     *
     * <p>Beispiel-Request:
     * <pre>
     * GET /api/hello
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     * </pre>
     *
     * <p>Beispiel-Response:
     * <pre>{ "message": "Hallo, admin!" }</pre>
     *
     * @param authentication Das Authentication-Objekt aus dem SecurityContext
     *                       (enthält Username und Authorities des eingeloggten Benutzers)
     * @return Personalisierter Gruß als JSON-Map
     */
    @GetMapping("/hello")
    public Map<String, String> hello(Authentication authentication) {
        // authentication.getName() gibt den Principal-Namen zurück,
        // d.h. den Benutzernamen aus dem JWT-Subject-Claim
        return Map.of("message", "Hallo, " + authentication.getName() + "!");
    }

    /**
     * Admin-geschützter Endpunkt – nur für Benutzer mit der Rolle {@code ROLE_ADMIN}.
     *
     * <p>{@code @PreAuthorize("hasRole('ADMIN')")} prüft die Rolle <b>vor</b> der
     * Methodenausführung. Intern wird aus {@code 'ADMIN'} die Authority {@code "ROLE_ADMIN"}
     * verglichen (Spring fügt das "ROLE_"-Präfix automatisch hinzu).
     *
     * <p>Bei fehlender Berechtigung wirft Spring Security eine
     * {@link org.springframework.security.access.AccessDeniedException} → HTTP 403 Forbidden.
     *
     * <p>Beispiel-Request (mit Admin-Token):
     * <pre>
     * GET /api/admin
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     * </pre>
     *
     * <p>Beispiel-Response:
     * <pre>{ "message": "Admin-Bereich: admin" }</pre>
     *
     * @param authentication Das Authentication-Objekt aus dem SecurityContext
     * @return Admin-Nachricht als JSON-Map
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")  // Nur ROLE_ADMIN darf diesen Endpunkt aufrufen
    public Map<String, String> adminOnly(Authentication authentication) {
        return Map.of("message", "Admin-Bereich: " + authentication.getName());
    }
}
