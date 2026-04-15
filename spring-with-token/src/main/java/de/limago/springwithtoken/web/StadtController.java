package de.limago.springwithtoken.web;

import de.limago.springwithtoken.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller mit ortsbasiertem Zugriffsschutz.
 *
 * <p>Demonstriert, wie anwendungsspezifische Informationen aus dem JWT-Token
 * (hier: der Wohnort) für Autorisierungsentscheidungen verwendet werden können –
 * über die übliche Rollenprüfung hinaus.
 */
@RestController
@RequestMapping("/api")
public class StadtController {

    /**
     * Endpunkt der nur für Benutzer mit Wohnort "Frankfurt" zugänglich ist.
     *
     * <p>Die Autorisierung erfolgt über {@code @PreAuthorize} mit einem SpEL-Ausdruck,
     * der die Methode {@link de.limago.springwithtoken.security.StadtSecurityService#istFrankfurt}
     * aufruft. Bei fehlender Berechtigung wirft Spring Security automatisch HTTP 403.
     *
     * <p>Der {@link AppUserDetails}-Cast ist sicher, weil {@code @PreAuthorize} die Methode
     * nur aufruft, wenn der Principal vom Typ {@code AppUserDetails} ist und "Frankfurt" zurückgibt.
     *
     * @param authentication Das Authentication-Objekt aus dem SecurityContext
     * @return Willkommensnachricht mit Wohnort-Information
     */
    @GetMapping("/frankfurt")
    @PreAuthorize("@stadtSecurity.istFrankfurt(authentication)")
    public Map<String, String> nurFuerFrankfurt(Authentication authentication) {
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        return Map.of(
                "message", "Willkommen, " + authentication.getName() + "!",
                "wohnort", userDetails.getWohnort(),
                "info",    "Dieser Bereich ist nur fuer Benutzer aus Frankfurt zugaenglich."
        );
    }
}
