package de.limago.onion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Sicherheitskonfiguration für Spring WebFlux.
 *
 * Rollenmodell:
 *   VORGESETZTER  → Hinz, Kunz   (dürfen alles)
 *   SACHBEARBEITER → Schmitt, Schulz (dürfen nur Adresskorrekturen)
 *
 * Die Zugriffsregeln auf Use-Case-Ebene werden durch @PreAuthorize
 * an den Service-Methoden durchgesetzt.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails hinz = User.withUsername("hinz")
                .password(passwordEncoder.encode("geheim"))
                .roles("VORGESETZTER")
                .build();

        UserDetails kunz = User.withUsername("kunz")
                .password(passwordEncoder.encode("geheim"))
                .roles("VORGESETZTER")
                .build();

        UserDetails schmitt = User.withUsername("schmitt")
                .password(passwordEncoder.encode("geheim"))
                .roles("SACHBEARBEITER")
                .build();

        UserDetails schulz = User.withUsername("schulz")
                .password(passwordEncoder.encode("geheim"))
                .roles("SACHBEARBEITER")
                .build();

        return new MapReactiveUserDetailsService(hinz, kunz, schmitt, schulz);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((exchange, authentication) -> {
                            var response = exchange.getExchange().getResponse();
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Onion\"");
                            return response.setComplete();
                        })
                )
                .build();
    }
}
