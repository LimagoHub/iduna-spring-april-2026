package de.limago.onion.infrastructure.adapter.outadapter.persistence.bankaccount;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bank_accounts")
@Data
@NoArgsConstructor
public class BankAccountDocument {

    @Id
    private String personId;

    @Version
    private Long version;

    private String iban;
    private String bic;
    private String bankName;
}
