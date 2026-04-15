package de.limago.onion.domain.tiere.schwein.aggregate;

import de.limago.onion.domain.shared.AggregateRoot;
import de.limago.onion.domain.tiere.schwein.event.ScheinDeletedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinCreatedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinMaxWeightReachedEvent;
import de.limago.onion.domain.tiere.schwein.event.SchweinUpdatedEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
@Setter(AccessLevel.PRIVATE)
@Getter
public class Schwein extends AggregateRoot {

    public static final int MAX_WEIGHT = 20;
    public static final int INITIAL_WEIGHT = 10;
    private String name;
    private int gewicht;
    private Instant deletedAt;

    private Schwein(final UUID id, final String name, final int gewicht) {
        super(id);
        setName(name);
        setGewicht(gewicht);

    }

    private void setName(final String name) {
        if(name.isBlank()) throw new IllegalArgumentException("Name darf nicht leer sein");
        if(name.equals(this.name)) return;
        if ("elsa".equalsIgnoreCase(name) ) throw new IllegalArgumentException("Name darf nicht Elsa sein");
        this.name = name;
        registerEvent(SchweinUpdatedEvent.of(getId(), name, gewicht));
    }

    private void setGewicht(final int gewicht) {
        if(this.gewicht == gewicht) return;
        if(gewicht < 0) throw new IllegalArgumentException("Gewicht darf nicht negativ sein");
        this.gewicht = gewicht;
        registerEvent(SchweinUpdatedEvent.of(getId(), name, gewicht));
        if(gewicht > MAX_WEIGHT) registerEvent(SchweinMaxWeightReachedEvent.of(getId(), name, gewicht));
    }

    public static Schwein create(UUID id, String name) {
        Schwein schwein = new Schwein(id, name, INITIAL_WEIGHT);
        schwein.registerEvent(SchweinCreatedEvent.of(id, name, INITIAL_WEIGHT));
        return schwein;
    }

    public static Schwein reconstitute(UUID id, Long version, String name, int gewicht,
                                                Instant deletedAt) {
        Schwein schwein = new Schwein(id, name, gewicht);
        schwein.setVersion(version);
        schwein.deletedAt = deletedAt;
        return schwein;

    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = Instant.now();
        registerEvent(ScheinDeletedEvent.of(getId(), name, gewicht));
    }

    public void taufen(final String name){
        setName(name);
    }

    public void fuettern() {
        setGewicht(getGewicht() + 1);
    }
}
