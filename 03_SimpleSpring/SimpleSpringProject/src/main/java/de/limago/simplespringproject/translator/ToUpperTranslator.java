package de.limago.simplespringproject.translator;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Qualifier("upper")

//@Primary

//@Profile("production")
public class ToUpperTranslator implements Translator{
    @Override
    public String translate(final String text) {
        return text.toUpperCase();
    }
}
