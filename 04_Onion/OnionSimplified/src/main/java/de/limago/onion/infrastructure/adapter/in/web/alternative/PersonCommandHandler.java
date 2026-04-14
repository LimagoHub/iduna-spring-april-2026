package de.limago.onion.infrastructure.adapter.in.web.alternative;

import de.limago.onion.application.commandside.command.person.CorrectPersonCommand;
import de.limago.onion.application.commandside.command.person.CreatePersonCommand;
import de.limago.onion.application.commandside.command.person.DeletePersonCommand;
import de.limago.onion.application.commandside.command.person.MovePersonCommand;
import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.inport.person.CorrectPersonUseCase;
import de.limago.onion.application.port.inport.person.CreatePersonUseCase;
import de.limago.onion.application.port.inport.person.DeletePersonUseCase;
import de.limago.onion.application.port.inport.person.MovePersonUseCase;
import de.limago.onion.domain.model.valueobject.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonCommandHandler {

    private final CreatePersonUseCase createPersonUseCase;
    private final MovePersonUseCase movePersonUseCase;
    private final CorrectPersonUseCase correctPersonUseCase;
    private final DeletePersonUseCase deletePersonUseCase;
    private final Validator validator;

    @Operation(
        tags = {"Person – Commands (v2)"},
        summary = "Person anlegen",
        description = """
            Legt eine neue Person an.

            **Client-generierte ID:** Die `id` kommt vom Client im Request-Body.
            Garantiert Idempotenz bei Wiederholungsversuchen.

            **Location-Header:** Response-Body leer, URL der neuen Ressource steht im
            `Location`-Header.
            """,
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CreatePersonRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Person angelegt",
            headers = @Header(name = "Location", description = "/api/v2/persons/{id}",
                schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe")
    })
    public Mono<ServerResponse> createPerson(ServerRequest request) {
        return request.bodyToMono(CreatePersonRequest.class)
                .flatMap(this::validated)
                .flatMap(body -> createPersonUseCase.createPerson(body.toCommand()))
                .flatMap(id -> ServerResponse
                        .created(URI.create("/api/v2/persons/" + id))
                        .build());
    }

    @Operation(
        tags = {"Person – Commands (v2)"},
        summary = "Stammdaten korrigieren",
        description = """
            Korrigiert Stammdaten einer Person. Alle Felder optional — nur gesetzte Felder
            werden geändert. Ein einziger Aufruf = eine Transaktion = kein Optimistic-Locking-Konflikt.
            """,
        parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid")),
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PatchPersonRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Korrektur durchgeführt"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden")
    })
    public Mono<ServerResponse> patchPerson(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return request.bodyToMono(PatchPersonRequest.class)
                .flatMap(this::validated)
                .flatMap(body -> correctPersonUseCase.correct(body.toCommand(id)))
                .then(ServerResponse.noContent().build())
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("PATCH /api/v2/persons/{} — {}", id, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }

    @Operation(
        tags = {"Person – Commands (v2)"},
        summary = "Person umziehen",
        description = """
            Registriert einen fachlichen Umzug als Domain Event (`PersonMovedEvent`).

            **Abweichung von PATCH /{id}:** Ein Umzug hat eigene fachliche Bedeutung und
            unterscheidet sich von einer Adresskorrektur (`PersonAddressCorrectedEvent`).
            `POST /{id}/moves` modelliert eine Sub-Ressource, kein RPC.
            """,
        parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid")),
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AddressRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Umzug registriert"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden")
    })
    public Mono<ServerResponse> movePerson(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return request.bodyToMono(AddressRequest.class)
                .flatMap(this::validated)
                .flatMap(body -> movePersonUseCase.movePerson(new MovePersonCommand(id, body.toDomain())))
                .then(ServerResponse.noContent().build())
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("POST /api/v2/persons/{}/moves — {}", id, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }

    @Operation(
        tags = {"Person – Commands (v2)"},
        summary = "Person löschen",
        description = "Soft Delete, kaskadiert auf das Bankkonto. Idempotent.",
        parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid"))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Person gelöscht"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden")
    })
    public Mono<ServerResponse> deletePerson(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return deletePersonUseCase.deletePerson(new DeletePersonCommand(id))
                .then(ServerResponse.noContent().build())
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("DELETE /api/v2/persons/{} — {}", id, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }

    // --- Validierungs-Helper ---

    private <T> Mono<T> validated(T body) {
        var violations = validator.validate(body);
        if (violations.isEmpty()) return Mono.just(body);
        return Mono.error(new ConstraintViolationException(violations));
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
