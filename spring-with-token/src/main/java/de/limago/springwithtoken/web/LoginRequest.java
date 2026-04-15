package de.limago.springwithtoken.web;

/**
 * Datentransferobjekt (DTO) für die Login-Anfrage.
 *
 * <p>Als Java {@code record} (seit Java 16) ist dieses Objekt:
 * <ul>
 *   <li><b>Unveränderlich</b> – alle Felder sind {@code final}</li>
 *   <li><b>Kompakt</b> – Konstruktor, Getter, {@code equals()}, {@code hashCode()},
 *       {@code toString()} werden automatisch generiert</li>
 * </ul>
 *
 * <p>Jackson (der JSON-Deserializer von Spring Boot) kann Records direkt verarbeiten.
 * Das folgende JSON wird automatisch in einen {@code LoginRequest} umgewandelt:
 * <pre>
 * {
 *   "username": "admin",
 *   "password": "admin"
 * }
 * </pre>
 *
 * <p>Die Getter heißen {@code username()} und {@code password()} (ohne "get"-Präfix).
 *
 * @param username Der Benutzername
 * @param password Das Passwort im Klartext (wird vom AuthenticationManager gegen den BCrypt-Hash geprüft)
 */
public record LoginRequest(String username, String password) {}
