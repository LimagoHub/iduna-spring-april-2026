package de.limago.onion.infrastructure.adapter.in.web.alternative;

import de.limago.onion.application.commandside.person.command.bankaccount.ChangeBankAccountCommand;
import de.limago.onion.application.commandside.person.command.bankaccount.CreateBankAccountCommand;
import de.limago.onion.application.shared.NotFoundException;
import de.limago.onion.application.commandside.person.port.inport.bankaccount.ChangeBankAccountUseCase;
import de.limago.onion.application.commandside.person.port.inport.bankaccount.CreateBankAccountUseCase;
import de.limago.onion.application.commandside.person.port.inport.bankaccount.FindBankAccountUseCase;
import de.limago.onion.application.queryside.query.bankaccount.FindBankAccountByPersonIdQuery;
import de.limago.onion.domain.person.valueobject.BankDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankAccountHandler {

    private final CreateBankAccountUseCase createBankAccountUseCase;
    private final ChangeBankAccountUseCase changeBankAccountUseCase;
    private final FindBankAccountUseCase findBankAccountUseCase;
    private final Validator validator;

    @Operation(
        tags = {"Bankkonto (v2)"},
        summary = "Bankkonto anlegen",
        description = "Legt ein neues Bankkonto für eine Person an. Pro Person ist genau ein Bankkonto möglich.",
        parameters = @Parameter(name = "personId", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid")),
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = BankDetailsRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bankkonto angelegt"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person nicht gefunden")
    })
    public Mono<ServerResponse> createBankAccount(ServerRequest request) {
        UUID personId = UUID.fromString(request.pathVariable("personId"));
        return request.bodyToMono(BankDetailsRequest.class)
                .flatMap(this::validated)
                .flatMap(body -> createBankAccountUseCase.createBankAccount(
                        new CreateBankAccountCommand(personId, body.toDomain())))
                .flatMap(id -> ServerResponse.status(HttpStatus.CREATED).bodyValue(id))
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("POST /api/v2/persons/{}/bank-account — {}", personId, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }

    @Operation(
        tags = {"Bankkonto (v2)"},
        summary = "Bankkonto aktualisieren",
        description = "Ersetzt die Bankverbindung vollständig (PUT-Semantik: alle Felder werden überschrieben).",
        parameters = @Parameter(name = "personId", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid")),
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = BankDetailsRequest.class))
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Bankkonto aktualisiert"),
        @ApiResponse(responseCode = "400", description = "Ungültige Eingabe"),
        @ApiResponse(responseCode = "404", description = "Person oder Bankkonto nicht gefunden")
    })
    public Mono<ServerResponse> changeBankAccount(ServerRequest request) {
        UUID personId = UUID.fromString(request.pathVariable("personId"));
        return request.bodyToMono(BankDetailsRequest.class)
                .flatMap(this::validated)
                .flatMap(body -> changeBankAccountUseCase.changeBankAccount(
                        new ChangeBankAccountCommand(personId, body.toDomain())))
                .then(ServerResponse.noContent().build())
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("PUT /api/v2/persons/{}/bank-account — {}", personId, e.getMessage());
                    return ServerResponse.notFound().build();
                });
    }

    @Operation(
        tags = {"Bankkonto (v2)"},
        summary = "Bankkonto laden",
        description = "Gibt die Bankverbindung einer Person zurück.",
        parameters = @Parameter(name = "personId", in = ParameterIn.PATH, description = "ID der Person",
            schema = @Schema(type = "string", format = "uuid"))
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bankkonto gefunden"),
        @ApiResponse(responseCode = "404", description = "Person oder Bankkonto nicht gefunden")
    })
    public Mono<ServerResponse> findByPersonId(ServerRequest request) {
        UUID personId = UUID.fromString(request.pathVariable("personId"));
        return findBankAccountUseCase.findByPersonId(new FindBankAccountByPersonIdQuery(personId))
                .flatMap(result -> ServerResponse.ok().bodyValue(result))
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("GET /api/v2/persons/{}/bank-account — nicht gefunden", personId))
                    .then(ServerResponse.notFound().build()));
    }

    // --- Validierungs-Helper ---

    private <T> Mono<T> validated(T body) {
        var violations = validator.validate(body);
        if (violations.isEmpty()) return Mono.just(body);
        return Mono.error(new ConstraintViolationException(violations));
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
