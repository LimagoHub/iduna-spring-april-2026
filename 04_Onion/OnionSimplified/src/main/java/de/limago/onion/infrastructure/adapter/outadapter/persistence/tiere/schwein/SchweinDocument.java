package de.limago.onion.infrastructure.adapter.outadapter.persistence.tiere.schwein;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "schweine")
@Data
@NoArgsConstructor
public class SchweinDocument {

    @Id
    private String id;

    @Version
    private Long version;

    private String name;
    private int gewicht;
    private Instant deletedAt;
}
