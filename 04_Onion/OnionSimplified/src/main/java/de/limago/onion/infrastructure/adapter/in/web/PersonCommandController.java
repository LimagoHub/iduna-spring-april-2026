package de.limago.onion.infrastructure.adapter.in.web;

import de.limago.onion.application.commandside.command.person.CorrectPersonCommand;
import de.limago.onion.application.commandside.command.person.CreatePersonCommand;
import de.limago.onion.application.commandside.command.person.DeletePersonCommand;
import de.limago.onion.application.commandside.command.person.MovePersonCommand;
import de.limago.onion.application.port.inport.person.CorrectPersonUseCase;
import de.limago.onion.application.port.inport.person.CreatePersonUseCase;
import de.limago.onion.application.port.inport.person.DeletePersonUseCase;
import de.limago.onion.application.port.inport.person.MovePersonUseCase;
import de.limago.onion.domain.model.valueobject.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

//  http://localhost:8080/v3/api-docs

@Tag(name = "Person – Commands (v1)", description = "Schreiboperationen auf Personen — annotationsbasierter Stil")
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonCommandController {

    private final CreatePersonUseCase createPersonUseCase;
    private final MovePersonUseCase movePersonUseCase;
    private final CorrectPersonUseCase correctPersonUseCase;
    private final DeletePersonUseCase deletePersonUseCase;

    @Operation(
        summary = "Person anlegen",
        description = """
            Legt eine neue Person an.

            **Client-generierte ID:** Die `id` kommt vom Client im Request-Body, nicht vom Server.
            Das garantiert Idempotenz — derselbe Request kann bei Netzwerkfehlern sicher wiederholt
            werden, ohne eine zweite Person zu erzeugen.

            **Location-Header:** Der Response-Body ist leer. Die URL der neuen Ressource steht
            im `Location`-Header (`201 Created`).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Person angelegt",
            headers = @Header(name = "Location", description = "/api/persons/{id}",
                schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "409", description = "Person mit dieser ID existiert bereits"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })

    @PostMapping
    public Mono<ResponseEntity<Void>> createPerson(@Valid @RequestBody CreatePersonRequest request) {
        return createPersonUseCase.createPerson(request.toCommand())
                .map(id -> ResponseEntity.created(URI.create("/api/persons/" + id)).build());
    }

    @Operation(
        summary = "Stammdaten korrigieren",
        description = """
            Korrigiert Stammdaten einer Person (Vorname, Nachname, Adresse).
            Alle Felder sind **optional** — nur gesetzte Felder werden geändert.

            **Warum ein einziges PATCH statt separate Endpunkte pro Feld:**
            Separate Aufrufe für Vorname, Nachname und Adresse würden drei Datenbankoperationen
            mit je einem `@Version`-Check erzeugen. Der zweite Aufruf würde mit einem
            Optimistic-Locking-Konflikt scheitern, wenn ein anderer Client zwischen den Aufrufen
            schreibt. Ein einziger PATCH-Aufruf = eine Transaktion = ein `@Version`-Check.

            **Abgrenzung zu POST /{id}/moves:**
            Dieser Endpunkt dient technischen Korrekturen (Tippfehler, falsche Daten).
            Ein fachlicher Wohnortwechsel wird über `POST /{id}/moves` abgebildet.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Korrektur durchgeführt"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Void>> patchPerson(
            @Parameter(description = "ID der Person") @PathVariable UUID id,
            @Valid @RequestBody PatchPersonRequest request) {
        return correctPersonUseCase.correct(request.toCommand(id))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @Operation(
        summary = "Person umziehen",
        description = """
            Registriert einen **fachlichen Umzug** als Domain Event.

            **Bewusste REST-Abweichung:** Obwohl sich nur die Adresse ändert, wird nicht
            `PATCH /{id}` verwendet, sondern ein eigener Endpunkt `POST /{id}/moves`.

            Grund: Ein Umzug ist ein eigenständiges fachliches Ereignis (`PersonMovedEvent`)
            mit anderer Downstream-Bedeutung als eine Adresskorrektur (`PersonAddressCorrectedEvent`).
            Die URL `/moves` modelliert eine Sub-Ressource — "ich erstelle einen Umzug
            für diese Person" — und ist kein RPC-Aufruf.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Umzug registriert"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PostMapping("/{id}/moves")
    public Mono<ResponseEntity<Void>> movePerson(
            @Parameter(description = "ID der Person") @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        return movePersonUseCase.movePerson(new MovePersonCommand(id, request.toDomain()))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @Operation(
        summary = "Person löschen",
        description = """
            Markiert eine Person als gelöscht (Soft Delete). Kaskadiert auf das Bankkonto.
            Idempotent — ein wiederholter Aufruf auf eine bereits gelöschte Person gibt
            ebenfalls `204` zurück und erzeugt kein weiteres Event.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Person gelöscht"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deletePerson(
            @Parameter(description = "ID der Person") @PathVariable UUID id) {
        return deletePersonUseCase.deletePerson(new DeletePersonCommand(id))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    // --- Request Bodies ---

    record CreatePersonRequest(
            @NotNull(message = "ID darf nicht null sein") UUID id,
            @NotBlank(message = "Vorname darf nicht leer sein") @Size(min = 2, max = 100, message = "Vorname muss 2–100 Zeichen haben") String firstName,
            @NotBlank(message = "Nachname darf nicht leer sein") @Size(min = 2, max = 100, message = "Nachname muss 2–100 Zeichen haben") String lastName,
            @NotNull(message = "Adresse darf nicht null sein") @Valid AddressRequest address
    ) {
        CreatePersonCommand toCommand() {
            return new CreatePersonCommand(id, firstName, lastName, address.toDomain());
        }
    }

    record PatchPersonRequest(
            @Size(min = 2, max = 100, message = "Vorname muss 2–100 Zeichen haben") String firstName,
            @Size(min = 2, max = 100, message = "Nachname muss 2–100 Zeichen haben") String lastName,
            @Valid AddressRequest address
    ) {
        CorrectPersonCommand toCommand(UUID id) {
            return new CorrectPersonCommand(
                    id,
                    Optional.ofNullable(firstName),
                    Optional.ofNullable(lastName),
                    Optional.ofNullable(address).map(AddressRequest::toDomain)
            );
        }
    }

    record AddressRequest(
            @NotBlank(message = "Straße darf nicht leer sein") String street,
            @NotBlank(message = "Hausnummer darf nicht leer sein") String houseNumber,
            @NotBlank(message = "Postleitzahl darf nicht leer sein")
            @Pattern(regexp = "\\d{4,10}", message = "Postleitzahl muss 4–10 Ziffern haben") String postalCode,
            @NotBlank(message = "Stadt darf nicht leer sein") String city,
            @NotBlank(message = "Land darf nicht leer sein") String country
    ) {
        Address toDomain() {
            return new Address(street, houseNumber, postalCode, city, country);
        }
    }
}
