package de.limago.onion.infrastructure.adapter.in.web;

import de.limago.onion.application.port.inport.person.FindPersonCompleteUseCase;
import de.limago.onion.application.port.inport.person.FindPersonUseCase;
import de.limago.onion.application.queryside.query.person.FindPersonByIdQuery;
import de.limago.onion.application.queryside.query.person.FindPersonCompleteByIdQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Person – Queries (v1)", description = "Leseoperationen auf Personen — annotationsbasierter Stil")
@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonQueryController {

    private final FindPersonUseCase findPersonUseCase;
    private final FindPersonCompleteUseCase findPersonCompleteUseCase;

    @Operation(
        summary = "Alle Personen laden",
        description = """
            Gibt alle Personen zurück.

            **Projektion statt Sub-Pfad:**
            Der Parameter `projection=complete` reichert jede Person mit ihren Bankdaten an,
            statt einen eigenen Endpunkt `/api/persons-complete` anzubieten.

            Begründung: Die URL identifiziert die **Ressource** (Person), nicht ihre Darstellung.
            Query-Parameter steuern *wie* eine Ressource zurückgegeben wird — das entspricht
            ihrem genauen REST-Zweck. `/persons-complete` würde fälschlicherweise einen eigenen
            Ressourcentyp suggerieren.

            | projection  | Enthält Bankdaten? | Response-Typ         |
            |-------------|-------------------|----------------------|
            | `default`   | nein              | `PersonResult`       |
            | `complete`  | ja                | `PersonCompleteResult` |
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste der Personen (ggf. mit Bankdaten)"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @GetMapping
    public Flux<Object> findAll(
            @Parameter(
                name = "projection",
                in = ParameterIn.QUERY,
                description = "Steuert den Detailgrad der Antwort. `complete` fügt Bankdaten hinzu.",
                schema = @Schema(type = "string", allowableValues = {"default", "complete"},
                    defaultValue = "default")
            )
            @RequestParam(defaultValue = "default") String projection) {
        if ("complete".equals(projection)) {
            return findPersonCompleteUseCase.findAllComplete().cast(Object.class);
        }
        return findPersonUseCase.findAll().cast(Object.class);
    }

    @Operation(
        summary = "Person laden",
        description = """
            Gibt eine einzelne Person zurück.

            **Projektion statt Sub-Pfad:**
            Dieselbe Logik wie bei `GET /api/persons` — `?projection=complete` liefert
            zusätzlich die Bankdaten, ohne einen eigenen URL-Pfad zu benötigen.

            | projection  | Enthält Bankdaten? | Response-Typ           |
            |-------------|-------------------|------------------------|
            | `default`   | nein              | `PersonResult`         |
            | `complete`  | ja                | `PersonCompleteResult` |
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Person gefunden"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> findById(
            @Parameter(description = "ID der Person") @PathVariable UUID id,
            @Parameter(
                name = "projection",
                in = ParameterIn.QUERY,
                description = "Steuert den Detailgrad der Antwort. `complete` fügt Bankdaten hinzu.",
                schema = @Schema(type = "string", allowableValues = {"default", "complete"},
                    defaultValue = "default")
            )
            @RequestParam(defaultValue = "default") String projection) {
        if ("complete".equals(projection)) {
            return findPersonCompleteUseCase.findCompleteById(new FindPersonCompleteByIdQuery(id))
                    .map(result -> ResponseEntity.ok((Object) result))
                    .defaultIfEmpty(ResponseEntity.notFound().build());
        }
        return findPersonUseCase.findById(new FindPersonByIdQuery(id))
                .map(result -> ResponseEntity.ok((Object) result))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
