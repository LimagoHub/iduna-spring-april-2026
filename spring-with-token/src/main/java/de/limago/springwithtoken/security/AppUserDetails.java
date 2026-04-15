package de.limago.springwithtoken.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Erweitertes {@link UserDetails}-Objekt mit anwendungsspezifischen Benutzerdaten.
 *
 * <p>Spring Security arbeitet intern mit dem {@link UserDetails}-Interface. Dieses Interface
 * enthält nur Basis-Informationen (Username, Passwort, Authorities). Um zusätzliche
 * Benutzerdaten – hier den <b>Wohnort</b> – durch die gesamte Security-Schicht
 * zu transportieren, wird das Standard-{@code UserDetails} mit dieser Klasse gewrappt
 * (Decorator-Pattern).
 *
 * <p>Der Wohnort wird:
 * <ul>
 *   <li>in {@link SecurityConfig} beim Anlegen der Benutzer gesetzt</li>
 *   <li>in {@link JwtService#generateToken} als Custom Claim {@code "wohnort"} ins JWT geschrieben</li>
 *   <li>von {@link StadtSecurityService} für die ortsbasierte Autorisierung gelesen</li>
 * </ul>
 */
public class AppUserDetails implements UserDetails {

    /** Das ursprüngliche UserDetails-Objekt, an das alle Standard-Methoden delegiert werden. */
    private final UserDetails delegate;

    /** Anwendungsspezifischer Zusatz: Wohnort des Benutzers. */
    private final String wohnort;

    /**
     * @param delegate Das von Spring erzeugte Standard-{@link UserDetails}-Objekt
     * @param wohnort  Der Wohnort des Benutzers (z.B. "Frankfurt", "Berlin")
     */
    public AppUserDetails(UserDetails delegate, String wohnort) {
        this.delegate = delegate;
        this.wohnort = wohnort;
    }

    /** @return Den Wohnort des Benutzers */
    public String getWohnort() {
        return wohnort;
    }

    // --- Delegation an das eingebettete UserDetails-Objekt ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return delegate.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return delegate.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return delegate.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }
}
