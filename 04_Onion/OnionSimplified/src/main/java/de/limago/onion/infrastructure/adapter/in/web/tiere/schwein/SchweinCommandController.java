package de.limago.onion.infrastructure.adapter.in.web.tiere.schwein;

import de.limago.onion.application.commandside.tiere.schwein.command.CreateSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.DeleteSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.FuetternSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.command.UpdateSchweinCommand;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.CreateSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.DeleteSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.FuetternSchweinUseCase;
import de.limago.onion.application.commandside.tiere.schwein.port.inport.UpdateSchweinUseCase;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Schwein – Commands", description = "Schreiboperationen auf Schweinen")
@RestController
@RequestMapping("/api/schweine")
@RequiredArgsConstructor
public class SchweinCommandController {

    private final CreateSchweinUseCase createSchweinUseCase;
    private final UpdateSchweinUseCase updateSchweinUseCase;
    private final DeleteSchweinUseCase deleteSchweinUseCase;
    private final FuetternSchweinUseCase fuetternSchweinUseCase;

    @Operation(
        summary = "Schwein anlegen",
        description = """
            Legt ein neues Schwein an. Die ID kommt vom Client (Idempotenz).
            Das Anfangsgewicht ist fest auf 10 kg gesetzt.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Schwein angelegt",
            headers = @Header(name = "Location", description = "/api/schweine/{id}",
                schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "409", description = "Schwein mit dieser ID existiert bereits"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PostMapping
    public Mono<ResponseEntity<Void>> createSchwein(@Valid @RequestBody CreateSchweinRequest request) {
        return createSchweinUseCase.createSchwein(request.toCommand())
                .map(id -> ResponseEntity.created(URI.create("/api/schweine/" + id)).<Void>build());
    }

    @Operation(
        summary = "Schwein umbenennen (taufen)",
        description = "Ändert den Namen des Schweins. Name 'Elsa' ist nicht erlaubt."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Schwein aktualisiert"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Schwein nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<Void>> updateSchwein(
            @Parameter(description = "ID des Schweins") @PathVariable UUID id,
            @Valid @RequestBody UpdateSchweinRequest request) {
        return updateSchweinUseCase.updateSchwein(request.toCommand(id))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @Operation(
        summary = "Schwein füttern",
        description = """
            Erhöht das Gewicht des Schweins um 1 kg.
            Die `fuetterungId` kommt vom Client und dient der späteren Idempotenz-Absicherung
            (doppelte Fütterungen mit gleicher ID sollen nur einmal wirken).
            Die Prüfung ist noch nicht implementiert — die ID wird lediglich durchgereicht.
            Wird kein Schwein mit der gegebenen ID gefunden, wird 404 zurückgegeben.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Schwein gefüttert"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Schwein nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PostMapping("/{id}/fuetterungen")
    public Mono<ResponseEntity<Void>> fuettern(
            @Parameter(description = "ID des Schweins") @PathVariable UUID id,
            @Valid @RequestBody FuetternRequest request) {
        return fuetternSchweinUseCase.fuettern(new FuetternSchweinCommand(id, request.fuetterungId()))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @Operation(
        summary = "Schwein löschen",
        description = "Markiert ein Schwein als gelöscht (Soft Delete)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Schwein gelöscht"),
        @ApiResponse(responseCode = "404", description = "Schwein nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteSchwein(
            @Parameter(description = "ID des Schweins") @PathVariable UUID id) {
        return deleteSchweinUseCase.deleteSchwein(new DeleteSchweinCommand(id))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    // --- Request Bodies ---

    record CreateSchweinRequest(
            @NotNull(message = "ID darf nicht null sein") UUID id,
            @NotBlank(message = "Name darf nicht leer sein") @Size(min = 1, max = 100, message = "Name muss 1–100 Zeichen haben") String name
    ) {
        CreateSchweinCommand toCommand() {
            return new CreateSchweinCommand(id, name, 0);
        }
    }

    record UpdateSchweinRequest(
            @Size(min = 1, max = 100, message = "Name muss 1–100 Zeichen haben") String name
    ) {
        UpdateSchweinCommand toCommand(UUID id) {
            return new UpdateSchweinCommand(id, Optional.ofNullable(name), Optional.empty());
        }
    }

    record FuetternRequest(
            @NotNull(message = "fuetterungId darf nicht null sein") UUID fuetterungId
    ) {}
}
