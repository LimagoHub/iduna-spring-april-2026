package de.limago.simplespringproject.demo;

import de.limago.simplespringproject.translator.Translator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
//@Lazy
//@Scope("prototype")
@Scope("singleton")
//@RequiredArgsConstructor
public class Demo {


    private final Translator translator;

    public Demo(Translator translator) {
        this.translator = translator;
        System.out.println(translator.translate("Hallo Konstruktor"));
    }

    @PostConstruct
    public void init() {
        System.out.println(translator.translate("Hallo Postconstruct"));
    }

    @PreDestroy
    public void destroy() {
        System.out.println("Das ist Destroy");
    }
}
