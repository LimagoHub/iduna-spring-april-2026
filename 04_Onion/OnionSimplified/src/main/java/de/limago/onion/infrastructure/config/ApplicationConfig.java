package de.limago.onion.infrastructure.config;


import de.limago.onion.application.commandside.service.BankAccountCommandService;
import de.limago.onion.application.commandside.service.PersonCommandService;
import de.limago.onion.application.port.outport.BankAccountRepository;
import de.limago.onion.application.port.outport.DomainEventPublisher;
import de.limago.onion.application.port.outport.PersonRepository;
import de.limago.onion.application.queryside.service.BankAccountQueryService;
import de.limago.onion.application.queryside.service.PersonQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ApplicationConfig {

    @Qualifier("bankRaeuber")
    @Bean
    public List<String> bankRaeuber() {
        return List.of("Peter", "Paul", "Mary");
    }

    @Qualifier("fruits")
    @Bean
    public List<String> fruits() {
        return List.of("Cherry", "Raspberry", "Banana");
    }
    @Bean
    public PersonCommandService personCommandService(
            PersonRepository personRepository,
            BankAccountRepository bankAccountRepository,
            DomainEventPublisher eventPublisher) {
        return new PersonCommandService(personRepository, bankAccountRepository, eventPublisher);
    }

    @Bean
    public PersonQueryService personQueryService(
            PersonRepository personRepository,
            BankAccountRepository bankAccountRepository) {
        return new PersonQueryService(personRepository, bankAccountRepository);
    }

    @Bean
    public BankAccountCommandService bankAccountCommandService(
            BankAccountRepository bankAccountRepository,
            DomainEventPublisher eventPublisher) {
        return new BankAccountCommandService(bankAccountRepository, eventPublisher);
    }

    @Bean
    public BankAccountQueryService bankAccountQueryService(
            BankAccountRepository bankAccountRepository) {
        return new BankAccountQueryService(bankAccountRepository);
    }
}
