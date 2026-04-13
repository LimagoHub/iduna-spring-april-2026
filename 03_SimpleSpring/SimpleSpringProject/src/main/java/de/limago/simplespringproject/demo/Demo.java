package de.limago.simplespringproject.demo;

import de.limago.simplespringproject.translator.Translator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
//@Lazy
//@Scope("prototype")
@Scope("singleton")
@RequiredArgsConstructor
public class Demo {

    @Qualifier("upper")
    private final Translator translator;

    @Value("${Demo.message}")
    private final String message ;



    @PostConstruct
    public void init() {
        System.out.println(translator.translate("Hallo Postconstruct"));
        System.out.println(message);
    }

    @PreDestroy
    public void destroy() {
        System.out.println("Das ist Destroy");
    }
}
