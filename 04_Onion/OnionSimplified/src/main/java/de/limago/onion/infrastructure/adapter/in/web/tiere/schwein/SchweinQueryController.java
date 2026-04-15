package de.limago.onion.infrastructure.adapter.in.web.tiere.schwein;

import de.limago.onion.application.commandside.tiere.schwein.port.inport.FindSchweinUseCase;
import de.limago.onion.application.queryside.query.schwein.FindSchweinByIdQuery;
import de.limago.onion.application.queryside.query.schwein.SchweinResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Schwein – Queries", description = "Leseoperationen auf Schweinen")
@RestController
@RequestMapping("/api/schweine")
@RequiredArgsConstructor
public class SchweinQueryController {

    private final FindSchweinUseCase findSchweinUseCase;

    @Operation(
        summary = "Alle Schweine laden",
        description = "Gibt alle Schweine zurück."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste der Schweine"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @GetMapping
    public Flux<SchweinResult> findAll() {
        return findSchweinUseCase.findAll();
    }

    @Operation(
        summary = "Schwein laden",
        description = "Gibt ein einzelnes Schwein anhand seiner ID zurück."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Schwein gefunden"),
        @ApiResponse(responseCode = "404", description = "Schwein nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<SchweinResult>> findById(
            @Parameter(description = "ID des Schweins") @PathVariable UUID id) {
        return findSchweinUseCase.findById(new FindSchweinByIdQuery(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
