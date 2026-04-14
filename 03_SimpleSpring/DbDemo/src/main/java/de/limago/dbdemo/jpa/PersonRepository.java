package de.limago.dbdemo.jpa;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface PersonRepository extends CrudRepository<PersonEntity, UUID> {


    Iterable<PersonEntity> findByVorname(String vorname);
}
