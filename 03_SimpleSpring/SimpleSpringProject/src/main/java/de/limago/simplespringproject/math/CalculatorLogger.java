package de.limago.simplespringproject.math;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Qualifier("logger")
public class CalculatorLogger implements Calculator {


    private final Calculator calculator;


    @Override
    public double add(final double a, final double b) {
        System.out.println("add(" + a + ", " + b + ")");
        return calculator.add(a, b);
    }

    @Override
    public double sub(final double a, final double b) {
        return 0;
    }
}
