package de.limago.springwithtoken.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet-Filter für die zustandslose JWT-Authentifizierung.
 *
 * <p>Dieser Filter wird von Spring Security <b>bei jeder eingehenden HTTP-Anfrage</b>
 * genau einmal ausgeführt – daher die Basisklasse {@link OncePerRequestFilter}.
 * Er läuft <i>vor</i> dem Standard-{@code UsernamePasswordAuthenticationFilter} und ist
 * für das Erkennen und Verarbeiten von Bearer-Tokens im {@code Authorization}-Header zuständig.
 *
 * <h3>Verarbeitungsablauf:</h3>
 * <ol>
 *   <li>Authorization-Header lesen</li>
 *   <li>Prüfen ob Header mit "Bearer " beginnt (sonst: weiterleiten ohne Authentifizierung)</li>
 *   <li>Token extrahieren und mit {@link JwtService#validateToken} prüfen</li>
 *   <li>Benutzername aus Token lesen</li>
 *   <li>UserDetails laden und Authentication in den SecurityContext setzen</li>
 *   <li>Nächsten Filter in der Kette aufrufen</li>
 * </ol>
 *
 * <h3>Warum {@code @Lazy} auf dem UserDetailsService?</h3>
 * Ohne {@code @Lazy} entsteht ein zirkulärer Abhängigkeits-Zyklus beim Start:
 * {@code SecurityConfig} → {@code UserDetailsService} → {@code JwtAuthenticationFilter}
 * → {@code UserDetailsService}. Die verzögerte Initialisierung bricht diesen Zyklus.
 *
 * @see JwtService
 * @see de.limago.springwithtoken.config.SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Verwendet für Token-Validierung und Username-Extraktion. */
    private final JwtService jwtService;

    /**
     * Lädt Benutzerdetails aus dem konfigurierten UserDetailsService.
     * {@code @Lazy} verhindert zirkuläre Abhängigkeiten beim Anwendungsstart.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Konstruktor – Spring injiziert die Abhängigkeiten automatisch.
     *
     * @param jwtService         Dienst zum Validieren und Lesen von Tokens
     * @param userDetailsService Dienst zum Laden von Benutzerdaten (lazy injiziert)
     */
    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Hauptlogik des Filters – wird bei jeder HTTP-Anfrage einmal aufgerufen.
     *
     * <p>Die Methode folgt dem "Fast-Fail"-Prinzip: Sobald feststeht, dass kein
     * gültiges Token vorhanden ist, wird direkt weitergeleitet ({@code filterChain.doFilter})
     * ohne eine Ausnahme zu werfen. Spring Security entscheidet dann später,
     * ob der unauthentifizierte Zugriff erlaubt ist (z.B. für {@code /auth/login}).
     *
     * @param request     Die eingehende HTTP-Anfrage
     * @param response    Die HTTP-Antwort
     * @param filterChain Die restliche Filter-Kette (nächster Filter oder Servlet)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization-Header lesen
        // Format erwartet: "Authorization: Bearer eyJhbGciOi..."
        String authHeader = request.getHeader("Authorization");

        // Kein Header vorhanden oder falsches Präfix → Anfrage ohne Authentifizierung weiterleiten.
        // Spring Security prüft danach, ob der Endpunkt öffentlich zugänglich ist.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Token extrahieren: "Bearer " hat 7 Zeichen → substring(7) gibt den reinen Token-String
        String token = authHeader.substring(7);

        // 3. Token validieren (Signatur + Ablaufzeit)
        // Bei ungültigem Token: Anfrage ohne Authentifizierung weiterleiten (kein Absturz)
        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Benutzernamen aus dem "sub"-Claim des Tokens lesen
        String username = jwtService.getUsernameFromToken(token);

        // 5. Authentifizierung nur setzen, wenn:
        //    - Ein Username vorhanden ist (sollte nach validateToken immer der Fall sein)
        //    - Der SecurityContext noch keine Authentifizierung enthält
        //      (verhindert doppeltes Setzen bei mehreren Filtern)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // UserDetails aus dem konfigurierten UserDetailsService laden
            // (hier: InMemoryUserDetailsManager aus SecurityConfig)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // wohnort-Claim aus dem Token lesen und AppUserDetails rekonstruieren.
            // InMemoryUserDetailsManager gibt intern ein neues User-Objekt zurück –
            // das AppUserDetails-Objekt geht dabei verloren. Da der wohnort aber im
            // JWT-Claim steckt, wird er hier aus dem Token wiederhergestellt.
            String wohnort = jwtService.getWohnortFromToken(token);
            if (wohnort != null) {
                userDetails = new AppUserDetails(userDetails, wohnort);
            }

            // UsernamePasswordAuthenticationToken erstellen:
            // - principal: UserDetails-Objekt (enthält Username + Authorities)
            // - credentials: null (Passwort wird nach Authentifizierung nicht mehr gebraucht)
            // - authorities: die Rollen/Berechtigungen des Benutzers
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // Zusätzliche Request-Details anhängen (IP-Adresse, Session-ID)
            // – nützlich für Audit-Logging und Sicherheitsevents
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Authentifizierung im SecurityContext speichern.
            // Alle nachfolgenden Filter und Controller können darauf zugreifen.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 6. Nächsten Filter in der Kette aufrufen (oder den eigentlichen Controller erreichen)
        filterChain.doFilter(request, response);
    }
}
