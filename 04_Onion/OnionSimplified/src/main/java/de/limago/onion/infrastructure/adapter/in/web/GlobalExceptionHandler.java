package de.limago.onion.infrastructure.adapter.in.web;

import de.limago.onion.application.commandside.person.exception.BankAccountServiceException;
import de.limago.onion.application.commandside.person.exception.PersonServiceException;
import de.limago.onion.application.commandside.tiere.schwein.exception.SchweinServiceException;
import de.limago.onion.application.shared.AlreadyExistsException;
import de.limago.onion.application.shared.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * Zentraler Fehler-Handler für alle annotationsbasierten Controller (v1).
 *
 * Gibt strukturierte Fehlerantworten im ProblemDetail-Format (RFC 7807) zurück.
 * Funktionale Endpoints (v2) behandeln bekannte Fehler explizit per .onErrorResume()
 * im jeweiligen Handler — unerwartete Fehler landen im Spring-Default-Handler.
 *
 * Validierungsfehler liefern ein `errors`-Array mit Feldname und Fehlermeldung:
 * { "errors": [{ "field": "firstName", "message": "darf nicht leer sein" }] }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Validierungsfehler (v1: @Valid auf @RequestBody) ---

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Validation failed [{}]: {}", exchange.getRequest().getPath(), errors);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validierungsfehler");
        problem.setDetail("Eingabe enthält ungültige Felder.");
        problem.setProperty("errors", errors);
        return problem;
    }

    // --- Validierungsfehler (v2: manuell per Validator in funktionalen Handlern) ---

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        List<FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        log.warn("Constraint violation [{}]: {}", exchange.getRequest().getPath(), errors);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validierungsfehler");
        problem.setDetail("Eingabe enthält ungültige Felder.");
        problem.setProperty("errors", errors);
        return problem;
    }

    // --- Fachliche Fehler ---

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, ServerWebExchange exchange) {
        log.warn("Not found [{}]: {}", exchange.getRequest().getPath(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Ressource nicht gefunden");
        return problem;
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(AlreadyExistsException ex, ServerWebExchange exchange) {
        log.warn("Already exists [{}]: {}", exchange.getRequest().getPath(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Ressource existiert bereits");
        return problem;
    }

    @ExceptionHandler(PersonServiceException.class)
    public ProblemDetail handlePersonServiceError(PersonServiceException ex, ServerWebExchange exchange) {
        log.error("PersonService error [{}]: {}", exchange.getRequest().getPath(), ex.getCause().getMessage(), ex.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ein technischer Fehler ist aufgetreten.");
        problem.setTitle("Interner Fehler");
        return problem;
    }

    @ExceptionHandler(BankAccountServiceException.class)
    public ProblemDetail handleBankAccountServiceError(BankAccountServiceException ex, ServerWebExchange exchange) {
        log.error("BankAccountService error [{}]: {}", exchange.getRequest().getPath(), ex.getCause().getMessage(), ex.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ein technischer Fehler ist aufgetreten.");
        problem.setTitle("Interner Fehler");
        return problem;
    }

    @ExceptionHandler(SchweinServiceException.class)
    public ProblemDetail handleSchweinServiceError(SchweinServiceException ex, ServerWebExchange exchange) {
        log.error("SchweinService error [{}]: {}", exchange.getRequest().getPath(), ex.getCause().getMessage(), ex.getCause());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ein technischer Fehler ist aufgetreten.");
        problem.setTitle("Interner Fehler");
        return problem;
    }

    // --- Unerwartete Fehler ---

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error [{}]: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ein interner Fehler ist aufgetreten.");
        problem.setTitle("Interner Fehler");
        return problem;
    }

    // --- DTO ---

    record FieldViolation(String field, String message) {}
}
