package de.limago.simplespringproject.math;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CalculatorImpl implements Calculator {

    @Override
    public double add(double a, double b) {
               return a + b;
    }

    @Override
    public double sub(double a, double b) {

        return add(a, -b);
    }
}
