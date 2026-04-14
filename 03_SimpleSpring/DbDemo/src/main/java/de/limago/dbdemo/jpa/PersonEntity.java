package de.limago.dbdemo.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_person")
public class PersonEntity {

    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, length = 50)
    private String vorname;
    @Column(nullable = false, length = 50)
    private String nachname;

}
