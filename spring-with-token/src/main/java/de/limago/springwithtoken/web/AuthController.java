package de.limago.springwithtoken.web;

import de.limago.springwithtoken.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST-Controller für den Authentifizierungs-Endpunkt.
 *
 * <p>Stellt den Login-Endpunkt {@code POST /auth/login} bereit.
 * Dieser Endpunkt ist in {@link de.limago.springwithtoken.config.SecurityConfig}
 * als öffentlich konfiguriert und erfordert daher <b>kein</b> JWT.
 *
 * <h3>Ablauf des Logins:</h3>
 * <ol>
 *   <li>Benutzername und Passwort werden aus dem Request-Body gelesen ({@link LoginRequest})</li>
 *   <li>{@link AuthenticationManager} prüft die Credentials gegen den {@link UserDetailsService}</li>
 *   <li>Bei Misserfolg wirft Spring Security automatisch eine Exception → HTTP 401</li>
 *   <li>Bei Erfolg wird ein JWT erstellt und im Response-Body zurückgegeben</li>
 * </ol>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * Koordiniert den Authentifizierungsvorgang.
     * Delegiert intern an den {@code DaoAuthenticationProvider},
     * der {@link UserDetailsService} und {@link org.springframework.security.crypto.password.PasswordEncoder}
     * verwendet.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Wird nach erfolgter Authentifizierung verwendet, um die vollständigen
     * {@link UserDetails} (inkl. Authorities/Rollen) zu laden, die ins JWT eingebettet werden.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Erstellt das JWT nach erfolgreicher Authentifizierung.
     */
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    /**
     * Login-Endpunkt: Prüft Credentials und gibt ein JWT zurück.
     *
     * <p>Beispiel-Request:
     * <pre>
     * POST /auth/login
     * Content-Type: application/json
     *
     * { "username": "admin", "password": "admin" }
     * </pre>
     *
     * <p>Beispiel-Response (HTTP 200):
     * <pre>
     * { "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.xxx" }
     * </pre>
     *
     * <p>Bei falschen Credentials: HTTP 401 (automatisch von Spring Security)
     *
     * @param request DTO mit {@code username} und {@code password} aus dem JSON-Body
     * @return HTTP 200 mit dem JWT im Body, oder HTTP 401 bei falschen Credentials
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {

        // authenticate() wirft eine AuthenticationException (→ HTTP 401),
        // wenn Benutzername oder Passwort falsch sind.
        // Bei Erfolg wird die Authentifizierung intern bestätigt, aber nicht genutzt –
        // wir laden UserDetails separat, um die aktuellen Authorities zu erhalten.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // UserDetails laden: enthält Username + Authorities (Rollen), die ins JWT eingebettet werden
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());

        // JWT erstellen – enthält sub (Username), roles, iat (jetzt) und exp (ablauf)
        String token = jwtService.generateToken(userDetails);

        // Token als JSON-Map zurückgeben: { "token": "eyJ..." }
        return ResponseEntity.ok(Map.of("token", token));
    }
}
