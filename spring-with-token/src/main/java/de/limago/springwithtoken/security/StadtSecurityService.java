package de.limago.springwithtoken.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Security-Bean für ortsbasierte Zugriffskontrolle.
 *
 * <p>Diese Klasse wird als Spring Bean mit dem Namen {@code "stadtSecurity"} registriert
 * und kann direkt in {@code @PreAuthorize}-Ausdrücken per SpEL (Spring Expression Language)
 * aufgerufen werden:
 *
 * <pre>
 * &#64;PreAuthorize("@stadtSecurity.istFrankfurt(authentication)")
 * </pre>
 *
 * <p>Das {@code @}-Zeichen verweist auf einen Spring Bean, {@code authentication} ist
 * das aktuelle {@link Authentication}-Objekt aus dem {@link org.springframework.security.core.context.SecurityContext},
 * das vom {@link JwtAuthenticationFilter} befüllt wurde.
 *
 * <p>Das Muster "Custom Security Bean + @PreAuthorize" ist der empfohlene Spring-Weg
 * für komplexe Autorisierungslogik, die über einfache Rollenprüfungen hinausgeht.
 */
@Service("stadtSecurity")  // Bean-Name muss mit dem @PreAuthorize-Ausdruck übereinstimmen
public class StadtSecurityService {

    /**
     * Prüft, ob der aktuell eingeloggte Benutzer in Frankfurt wohnt.
     *
     * <p>Das {@link Authentication#getPrincipal()} liefert das {@link AppUserDetails}-Objekt,
     * das beim Login vom {@code UserDetailsService} geladen und vom
     * {@link JwtAuthenticationFilter} in den SecurityContext gesetzt wurde.
     *
     * @param authentication Das aktuelle Authentication-Objekt aus dem SecurityContext
     * @return {@code true} wenn der Wohnort des Benutzers "Frankfurt" ist (Groß-/Kleinschreibung egal)
     */
    public boolean istFrankfurt(Authentication authentication) {
        // instanceof-Pattern-Matching (Java 16+): prüft Typ und castet in einem Schritt
        if (authentication.getPrincipal() instanceof AppUserDetails userDetails) {
            return "Frankfurt".equalsIgnoreCase(userDetails.getWohnort());
        }
        // Kein AppUserDetails-Principal → kein Wohnort bekannt → kein Zugang
        return false;
    }
}
