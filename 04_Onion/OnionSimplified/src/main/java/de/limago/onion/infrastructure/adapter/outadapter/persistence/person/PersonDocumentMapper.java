package de.limago.onion.infrastructure.adapter.outadapter.persistence.person;

import de.limago.onion.domain.model.aggregate.PersonAggregate;
import de.limago.onion.domain.model.valueobject.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PersonDocumentMapper {

    @Mapping(target = "id", expression = "java(person.getId().toString())")
    PersonDocument toDocument(PersonAggregate person);

    Address toDomainAddress(PersonDocument.AddressData data);

    default PersonAggregate toDomain(PersonDocument doc) {
        return PersonAggregate.reconstitute(
                UUID.fromString(doc.getId()),
                doc.getVersion(),
                doc.getFirstName(),
                doc.getLastName(),
                toDomainAddress(doc.getAddress()),
                doc.getDeletedAt()
        );
    }
}
