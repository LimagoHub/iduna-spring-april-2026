package de.limago.onion.infrastructure.adapter.in.web.alternative;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class V2Router {

    private final PersonCommandHandler commandHandler;
    private final PersonQueryHandler queryHandler;
    private final BankAccountHandler bankAccountHandler;

    @RouterOperations({
        // ----- Person – Queries -----
        @RouterOperation(
            path = "/api/v2/persons", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE,
            beanClass = PersonQueryHandler.class, beanMethod = "findAll"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{id}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE,
            beanClass = PersonQueryHandler.class, beanMethod = "findById"
        ),
        // ----- Person – Commands -----
        @RouterOperation(
            path = "/api/v2/persons", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            beanClass = PersonCommandHandler.class, beanMethod = "createPerson"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{id}", method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            beanClass = PersonCommandHandler.class, beanMethod = "patchPerson"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{id}/moves", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            beanClass = PersonCommandHandler.class, beanMethod = "movePerson"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{id}", method = RequestMethod.DELETE,
            beanClass = PersonCommandHandler.class, beanMethod = "deletePerson"
        ),
        // ----- BankAccount -----
        @RouterOperation(
            path = "/api/v2/persons/{personId}/bank-account", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE,
            beanClass = BankAccountHandler.class, beanMethod = "findByPersonId"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{personId}/bank-account", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            beanClass = BankAccountHandler.class, beanMethod = "createBankAccount"
        ),
        @RouterOperation(
            path = "/api/v2/persons/{personId}/bank-account", method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            beanClass = BankAccountHandler.class, beanMethod = "changeBankAccount"
        )
    })
    @Bean
    public RouterFunction<ServerResponse> v2Routes() {
        return RouterFunctions.route()
                // Person – Queries
                .GET("/api/v2/persons",              queryHandler::findAll)
                .GET("/api/v2/persons/{id}",         queryHandler::findById)
                // Person – Commands
                .POST("/api/v2/persons",             commandHandler::createPerson)
                .PATCH("/api/v2/persons/{id}",       commandHandler::patchPerson)
                .POST("/api/v2/persons/{id}/moves",  commandHandler::movePerson)
                .DELETE("/api/v2/persons/{id}",      commandHandler::deletePerson)
                // BankAccount
                .GET("/api/v2/persons/{personId}/bank-account",  bankAccountHandler::findByPersonId)
                .POST("/api/v2/persons/{personId}/bank-account", bankAccountHandler::createBankAccount)
                .PUT("/api/v2/persons/{personId}/bank-account",  bankAccountHandler::changeBankAccount)
                .build();
    }
}
