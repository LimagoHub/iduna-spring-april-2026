package de.limago.simplespringproject.translator;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
//@Qualifier("lower")
@Profile({"dev", "test"})
public class ToLowerTranslator implements Translator{
    @Override
    public String translate(final String text) {
        return text.toLowerCase();
    }
}
