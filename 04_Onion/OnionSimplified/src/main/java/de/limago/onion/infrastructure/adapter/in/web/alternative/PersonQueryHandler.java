package de.limago.onion.infrastructure.adapter.in.web.alternative;

import de.limago.onion.application.exception.NotFoundException;
import de.limago.onion.application.port.inport.person.FindPersonCompleteUseCase;
import de.limago.onion.application.port.inport.person.FindPersonUseCase;
import de.limago.onion.application.queryside.query.person.FindPersonByIdQuery;
import de.limago.onion.application.queryside.query.person.FindPersonCompleteByIdQuery;
import de.limago.onion.application.queryside.query.person.PersonCompleteResult;
import de.limago.onion.application.queryside.query.person.PersonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonQueryHandler {

    private final FindPersonUseCase findPersonUseCase;
    private final FindPersonCompleteUseCase findPersonCompleteUseCase;

    @Operation(
        tags = {"Person – Queries (v2)"},
        summary = "Alle Personen laden",
        description = """
            Gibt alle Personen zurück.

            **Projektion statt Sub-Pfad:**
            `?projection=complete` reichert jede Person mit Bankdaten an — ohne einen
            eigenen Endpunkt. Die URL identifiziert die Ressource (Person), der Query-Parameter
            steuert nur die Darstellung.

            | projection  | Bankdaten? | Response-Typ           |
            |-------------|-----------|------------------------|
            | `default`   | nein      | `PersonResult`         |
            | `complete`  | ja        | `PersonCompleteResult` |
            """,
        parameters = @Parameter(
            name = "projection", in = ParameterIn.QUERY,
            description = "Steuert den Detailgrad. `complete` fügt Bankdaten hinzu.",
            schema = @Schema(type = "string",
                allowableValues = {"default", "complete"}, defaultValue = "default")
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste der Personen")
    })
    public Mono<ServerResponse> findAll(ServerRequest request) {
        String projection = request.queryParam("projection").orElse("default");
        if ("complete".equals(projection)) {
            return ServerResponse.ok()
                    .body(findPersonCompleteUseCase.findAllComplete(), PersonCompleteResult.class);
        }
        return ServerResponse.ok()
                .body(findPersonUseCase.findAll(), PersonResult.class);
    }

    @Operation(
        tags = {"Person – Queries (v2)"},
        summary = "Person laden",
        description = """
            Gibt eine einzelne Person zurück.

            **Projektion statt Sub-Pfad:** Wie bei `GET /api/v2/persons` — `?projection=complete`
            liefert zusätzlich Bankdaten ohne eigenen URL-Pfad.
            """,
        parameters = {
            @Parameter(name = "id", in = ParameterIn.PATH, description = "ID der Person",
                schema = @Schema(type = "string", format = "uuid")),
            @Parameter(
                name = "projection", in = ParameterIn.QUERY,
                description = "Steuert den Detailgrad. `complete` fügt Bankdaten hinzu.",
                schema = @Schema(type = "string",
                    allowableValues = {"default", "complete"}, defaultValue = "default")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Person gefunden"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden")
    })
    public Mono<ServerResponse> findById(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        String projection = request.queryParam("projection").orElse("default");
        if ("complete".equals(projection)) {
            return findPersonCompleteUseCase.findCompleteById(new FindPersonCompleteByIdQuery(id))
                    .flatMap(result -> ServerResponse.ok().bodyValue(result))
                    .switchIfEmpty(Mono.fromRunnable(() -> log.warn("GET /api/v2/persons/{} (complete) — nicht gefunden", id))
                        .then(ServerResponse.notFound().build()))
                    .onErrorResume(NotFoundException.class, e -> {
                        log.warn("GET /api/v2/persons/{} (complete) — {}", id, e.getMessage());
                        return ServerResponse.notFound().build();
                    });
        }
        return findPersonUseCase.findById(new FindPersonByIdQuery(id))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("GET /api/v2/persons/{} — nicht gefunden", id))
                    .then(ServerResponse.notFound().build()))
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("GET /api/v2/persons/{} — {}", id, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }
}
