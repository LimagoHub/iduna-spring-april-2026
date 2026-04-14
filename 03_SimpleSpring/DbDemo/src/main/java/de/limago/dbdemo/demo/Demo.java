package de.limago.dbdemo.demo;


import de.limago.dbdemo.jpa.PersonEntity;
import de.limago.dbdemo.jpa.PersonRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Demo {

    private final PersonRepository personRepository;

    @PostConstruct
    public void foo() {

        PersonEntity person = PersonEntity.builder().vorname("John").nachname("Rambo").id(UUID.randomUUID()).build();

        personRepository.save(person);

        personRepository.findByVorname("John").forEach(System.out::println);
    }
}
