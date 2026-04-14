package de.limago.onion.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;

/**
 * Registriert den ReactiveMongoTransactionManager nur wenn ein Replica Set verfügbar ist.
 *
 * Kein @EnableTransactionManagement hier — Spring Boot aktiviert das automatisch via
 * TransactionAutoConfiguration, aber nur wenn ein TransactionManager-Bean vorhanden ist
 * (@ConditionalOnBean). Ohne Bean → kein TX-Management → @Transactional ist echtes No-Op.
 *
 * Lokalentwicklung (Standalone-MongoDB): spring.data.mongodb.transactions.enabled=false (default)
 * Produktion (Replica Set):               spring.data.mongodb.transactions.enabled=true
 */
@Configuration
public class TransactionConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.data.mongodb.transactions.enabled", havingValue = "true")
    public ReactiveMongoTransactionManager mongoTransactionManager(ReactiveMongoDatabaseFactory dbFactory) {
        return new ReactiveMongoTransactionManager(dbFactory);
    }
}
