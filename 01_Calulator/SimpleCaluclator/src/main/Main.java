package main;

import client.CalcClient;
import math.Calculator;
import math.CalculatorImpl;

public class Main {
    public static void main(String[] args) {
        Calculator calculator = new CalculatorImpl();
        calculator = new math.CalculatorLogger(calculator);
        calculator = new math.CalcSecure(calculator);
        CalcClient client = new CalcClient(calculator);
        client.go();
    }
}
