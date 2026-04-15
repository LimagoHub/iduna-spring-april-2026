package de.limago.onion.infrastructure.adapter.outadapter.persistence.tiere.schwein;

import de.limago.onion.domain.tiere.schwein.aggregate.Schwein;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SchweinDocumentMapper {

    @Mapping(target = "id", expression = "java(schwein.getId().toString())")
    SchweinDocument toDocument(Schwein schwein);

    default Schwein toDomain(SchweinDocument doc) {
        return Schwein.reconstitute(
                UUID.fromString(doc.getId()),
                doc.getVersion(),
                doc.getName(),
                doc.getGewicht(),
                doc.getDeletedAt()
        );
    }
}
