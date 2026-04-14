package de.limago.onion.infrastructure.adapter.outadapter.persistence.person;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "persons")
@Data
@NoArgsConstructor
public class PersonDocument {

    @Id
    private String id;

    @Version
    private Long version;

    private String firstName;
    private String lastName;
    private AddressData address;
    private Instant deletedAt;

    @Data
    @NoArgsConstructor
    public static class AddressData {
        private String street;
        private String houseNumber;
        private String postalCode;
        private String city;
        private String country;
    }
}
