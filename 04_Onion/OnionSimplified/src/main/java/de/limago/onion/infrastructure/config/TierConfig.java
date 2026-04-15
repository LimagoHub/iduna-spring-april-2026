package de.limago.onion.infrastructure.config;


import de.limago.onion.application.commandside.tiere.schwein.port.outport.SchweinRepository;
import de.limago.onion.application.commandside.tiere.schwein.service.SchweinCommandService;
import de.limago.onion.application.queryside.service.SchweinQueryService;
import de.limago.onion.application.shared.DomainEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TierConfig {


    @Bean
    public SchweinCommandService schweinCommandService(
            SchweinRepository schweinRepository,
            DomainEventPublisher eventPublisher) {
        return new SchweinCommandService(schweinRepository, eventPublisher);
    }

    @Bean
    public SchweinQueryService schweinQueryService(
            SchweinRepository schweinRepository) {
        return new SchweinQueryService(schweinRepository);
    }
}
