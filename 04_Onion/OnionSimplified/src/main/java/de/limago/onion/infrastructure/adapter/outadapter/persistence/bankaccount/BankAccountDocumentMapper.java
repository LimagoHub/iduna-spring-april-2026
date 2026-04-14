package de.limago.onion.infrastructure.adapter.outadapter.persistence.bankaccount;

import de.limago.onion.domain.model.aggregate.BankAccountAggregate;
import de.limago.onion.domain.model.valueobject.BankDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BankAccountDocumentMapper {

    @Mapping(target = "personId", expression = "java(account.getPersonId().toString())")
    @Mapping(target = "iban",     source = "bankDetails.iban")
    @Mapping(target = "bic",      source = "bankDetails.bic")
    @Mapping(target = "bankName", source = "bankDetails.bankName")
    BankAccountDocument toDocument(BankAccountAggregate account);

    default BankAccountAggregate toDomain(BankAccountDocument doc) {
        return BankAccountAggregate.reconstitute(
                UUID.fromString(doc.getPersonId()),
                doc.getVersion(),
                new BankDetails(doc.getIban(), doc.getBic(), doc.getBankName())
        );
    }
}
