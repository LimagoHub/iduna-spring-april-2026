package de.limago.springwithtoken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Spring-Boot-Anwendung "spring-with-token".
 *
 * <p>{@code @SpringBootApplication} ist eine Komfort-Annotation, die folgendes kombiniert:
 * <ul>
 *   <li>{@code @Configuration} – markiert diese Klasse als Konfigurationsquelle für Beans</li>
 *   <li>{@code @EnableAutoConfiguration} – aktiviert Spring Boots Auto-Konfiguration
 *       (z.B. Tomcat, Jackson, Spring Security werden automatisch konfiguriert)</li>
 *   <li>{@code @ComponentScan} – durchsucht das aktuelle Paket und alle Unterpakete nach
 *       Spring-Beans ({@code @Component}, {@code @Service}, {@code @Controller} etc.)</li>
 * </ul>
 *
 * <p>Die Anwendung startet einen eingebetteten Tomcat-Server auf Port 8080
 * (konfigurierbar in {@code application.yml}).
 */
@SpringBootApplication
public class SpringWithTokenApplication {

    /**
     * Java-Einstiegspunkt – startet den Spring-Application-Context.
     *
     * @param args Kommandozeilenargumente (können Spring-Properties überschreiben,
     *             z.B. {@code --server.port=9090})
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringWithTokenApplication.class, args);
    }
}
