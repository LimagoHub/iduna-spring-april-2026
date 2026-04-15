package de.limago.springwithtoken.config;

import de.limago.springwithtoken.security.AppUserDetails;
import de.limago.springwithtoken.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

/**
 * Zentrale Spring-Security-Konfiguration der Anwendung.
 *
 * <p>Diese Klasse ersetzt die frühere XML-Konfiguration und die veraltete
 * {@code WebSecurityConfigurerAdapter}-Erweiterung (ab Spring Security 5.7 deprecated).
 * Stattdessen werden Beans direkt deklariert.
 *
 * <h3>Aktivierte Annotationen:</h3>
 * <ul>
 *   <li>{@code @EnableWebSecurity} – aktiviert Spring Security für Web-Anfragen</li>
 *   <li>{@code @EnableMethodSecurity} – erlaubt {@code @PreAuthorize} und {@code @PostAuthorize}
 *       auf Methoden-Ebene (z.B. in {@link de.limago.springwithtoken.web.DemoController})</li>
 * </ul>
 *
 * @see de.limago.springwithtoken.security.JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Der JWT-Filter wird in die Spring-Security-Filterkette eingebunden.
     * Wird im Konstruktor injiziert (constructor injection = empfohlene Praxis).
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Definiert die Benutzerverwaltung für diese Demo-Anwendung.
     *
     * <p>Für Produktionsanwendungen wird hier typischerweise ein
     * {@code JdbcUserDetailsManager} (Datenbankanbindung) oder ein
     * eigener Service verwendet, der {@link UserDetailsService} implementiert.
     *
     * <p>Die Passwörter werden mit BCrypt gehasht gespeichert – niemals im Klartext.
     *
     * @param encoder Der PasswordEncoder-Bean (BCrypt), wird automatisch injiziert
     * @return Ein In-Memory-UserDetailsService mit zwei Testnutzern:
     *         <ul>
     *           <li>admin / admin → Rolle ADMIN</li>
     *           <li>user / user   → Rolle USER</li>
     *         </ul>
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        // AppUserDetails-Objekte in einer Map speichern und direkt zurückgeben.
        // InMemoryUserDetailsManager würde intern ein neues User-Objekt erzeugen und
        // dabei den Wohnort verlieren – deshalb eine eigene Implementierung.
        Map<String, AppUserDetails> store = Map.of(
                "admin", new AppUserDetails(
                        User.withUsername("admin")
                                .password(encoder.encode("admin"))
                                .roles("ADMIN")
                                .build(),
                        "Frankfurt"  // Wohnort → darf auf /api/frankfurt zugreifen
                ),
                "user", new AppUserDetails(
                        User.withUsername("user")
                                .password(encoder.encode("user"))
                                .roles("USER")
                                .build(),
                        "Berlin"     // Wohnort → wird von /api/frankfurt abgewiesen (HTTP 403)
                )
        );

        return username -> {
            AppUserDetails found = store.get(username);
            if (found == null) throw new UsernameNotFoundException(username);
            return found;
        };
    }

    /**
     * Definiert den Passwort-Hashalgorithmus.
     *
     * <p>BCrypt ist der empfohlene Algorithmus für Passwörter, weil er:
     * <ul>
     *   <li>einen eingebauten Salt enthält (verhindert Rainbow-Table-Angriffe)</li>
     *   <li>absichtlich langsam ist (erhöhter Aufwand für Brute-Force)</li>
     *   <li>den Kostenfaktor (Stärke) konfigurierbar macht (Standard: 10 Runden)</li>
     * </ul>
     *
     * @return Ein {@link BCryptPasswordEncoder} mit Standardstärke (Kostenfaktor 10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Stellt den {@link AuthenticationManager} als Bean bereit.
     *
     * <p>Der {@code AuthenticationManager} ist der zentrale Einstiegspunkt für
     * Authentifizierungsvorgänge. Er delegiert an einen oder mehrere
     * {@code AuthenticationProvider} (standardmäßig: {@code DaoAuthenticationProvider},
     * der {@code UserDetailsService} + {@code PasswordEncoder} verwendet).
     *
     * <p>Wird vom {@link de.limago.springwithtoken.web.AuthController} benötigt,
     * um Credentials programmatisch zu prüfen.
     *
     * @param config Spring-interne {@link AuthenticationConfiguration}
     * @return Den konfigurierten {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Konfiguriert die HTTP-Sicherheitsregeln (die eigentliche Security-Filterkette).
     *
     * <h3>Wichtige Entscheidungen:</h3>
     * <ul>
     *   <li><b>CSRF deaktiviert:</b> REST-APIs mit JWT sind nicht anfällig für CSRF,
     *       da keine Browser-Cookies zur Authentifizierung verwendet werden. CSRF-Tokens
     *       wären für reine API-Clients (z.B. mobile Apps) auch nicht handhabbar.</li>
     *
     *   <li><b>STATELESS Sessions:</b> Spring Security soll keine HTTP-Session anlegen.
     *       Jede Anfrage wird anhand des JWT vollständig authentifiziert – der Server
     *       merkt sich nichts zwischen Anfragen (zustandslos = stateless).</li>
     *
     *   <li><b>Endpunkt-Regeln:</b> Nur {@code /auth/login} ist öffentlich;
     *       alle anderen Anfragen erfordern Authentifizierung.</li>
     *
     *   <li><b>Filter-Reihenfolge:</b> Der {@link JwtAuthenticationFilter} wird VOR dem
     *       Standard-{@link UsernamePasswordAuthenticationFilter} eingefügt, damit das JWT
     *       ausgewertet wird, bevor Spring Security eigene Authentifizierungslogik ausführt.</li>
     * </ul>
     *
     * @param http Der {@link HttpSecurity}-Builder
     * @return Die konfigurierte {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF-Schutz deaktivieren (nicht nötig für stateless JWT-APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // Session-Management auf STATELESS setzen:
                // Spring Security legt keine HttpSession an, kein JSESSIONID-Cookie
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Autorisierungsregeln für HTTP-Anfragen
                .authorizeHttpRequests(auth -> auth
                        // Login-Endpunkt ist öffentlich (sonst kann sich niemand einloggen)
                        .requestMatchers("/auth/login", "/", "/index.html").permitAll()
                        // Alle anderen Anfragen erfordern einen gültigen JWT
                        .anyRequest().authenticated()
                )

                // JWT-Filter VOR dem UsernamePasswordAuthenticationFilter einfügen.
                // Damit wird das Token bei jeder Anfrage geprüft, bevor Spring Security
                // seinen eigenen Filter ausführt.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
