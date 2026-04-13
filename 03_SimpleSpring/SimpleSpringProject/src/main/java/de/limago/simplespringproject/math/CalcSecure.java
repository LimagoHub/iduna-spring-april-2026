package de.limago.simplespringproject.math;

public class CalcSecure implements Calculator{

    private final Calculator calculator;

    public CalcSecure(final Calculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public double add(final double a, final double b) {
        System.out.println("Du kommst hier rein");
        return calculator.add(a, b);
    }

    @Override
    public double sub(final double a, final double b) {
        return 0;
    }
}
