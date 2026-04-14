package de.limago.onion.infrastructure.adapter.in.web;

import de.limago.onion.application.commandside.command.bankaccount.ChangeBankAccountCommand;
import de.limago.onion.application.commandside.command.bankaccount.CreateBankAccountCommand;
import de.limago.onion.application.port.inport.bankaccount.ChangeBankAccountUseCase;
import de.limago.onion.application.port.inport.bankaccount.CreateBankAccountUseCase;
import de.limago.onion.application.port.inport.bankaccount.FindBankAccountUseCase;
import de.limago.onion.application.queryside.query.bankaccount.BankAccountResult;
import de.limago.onion.application.queryside.query.bankaccount.FindBankAccountByPersonIdQuery;
import de.limago.onion.domain.model.valueobject.BankDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Bankkonto (v1)", description = "CRUD-Operationen auf dem Bankkonto einer Person — annotationsbasierter Stil")
@RestController
@RequestMapping("/api/persons/{personId}/bank-account")
@RequiredArgsConstructor
public class BankAccountController {

    private final CreateBankAccountUseCase createBankAccountUseCase;
    private final ChangeBankAccountUseCase changeBankAccountUseCase;
    private final FindBankAccountUseCase findBankAccountUseCase;

    @Operation(
        summary = "Bankkonto anlegen",
        description = "Legt ein neues Bankkonto für eine Person an. Pro Person ist genau ein Bankkonto möglich."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bankkonto angelegt"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden"),
        @ApiResponse(responseCode = "409", description = "Bankkonto für diese Person existiert bereits"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PostMapping
    public Mono<ResponseEntity<UUID>> createBankAccount(
            @Parameter(description = "ID der Person") @PathVariable UUID personId,
            @Valid @RequestBody BankDetailsRequest request) {
        return createBankAccountUseCase.createBankAccount(
                new CreateBankAccountCommand(personId, request.toDomain()))
                .map(id -> ResponseEntity.status(HttpStatus.CREATED).body(id));
    }

    @Operation(
        summary = "Bankkonto aktualisieren",
        description = """
            Ersetzt die Bankverbindung einer Person vollständig (PUT-Semantik: alle Felder
            werden überschrieben).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Bankkonto aktualisiert"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person oder Bankkonto nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @PutMapping
    public Mono<ResponseEntity<Void>> changeBankAccount(
            @Parameter(description = "ID der Person") @PathVariable UUID personId,
            @Valid @RequestBody BankDetailsRequest request) {
        return changeBankAccountUseCase.changeBankAccount(
                new ChangeBankAccountCommand(personId, request.toDomain()))
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }

    @Operation(
        summary = "Bankkonto laden",
        description = "Gibt die Bankverbindung einer Person zurück."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bankkonto gefunden"),
        @ApiResponse(responseCode = "404", description = "Person oder Bankkonto nicht gefunden"),
        @ApiResponse(responseCode = "500", description = "Interner Serverfehler")
    })
    @GetMapping
    public Mono<ResponseEntity<BankAccountResult>> findByPersonId(
            @Parameter(description = "ID der Person") @PathVariable UUID personId) {
        return findBankAccountUseCase.findByPersonId(new FindBankAccountByPersonIdQuery(personId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- Request Body ---

    record BankDetailsRequest(
            @NotBlank(message = "IBAN darf nicht leer sein") @Size(min = 15, max = 34, message = "IBAN muss 15–34 Zeichen haben") String iban,
            @NotBlank(message = "BIC darf nicht leer sein") @Pattern(regexp = "[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?", message = "Ungültiger BIC-Code") String bic,
            @NotBlank(message = "Bankname darf nicht leer sein") String bankName
    ) {
        BankDetails toDomain() {
            return new BankDetails(iban, bic, bankName);
        }
    }
}
