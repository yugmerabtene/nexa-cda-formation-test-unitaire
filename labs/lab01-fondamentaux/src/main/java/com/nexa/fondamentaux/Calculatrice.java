package com.nexa.fondamentaux;

public class Calculatrice {

    public int addition(int a, int b) {
        return a + b;
    }

    public int soustraction(int a, int b) {
        return a - b;
    }

    public int multiplication(int a, int b) {
        return a * b;
    }

    public int division(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division par zéro interdite");
        }
        return a / b;
    }

    public int modulo(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Modulo par zéro interdit");
        }
        return a % b;
    }

    public boolean estPair(int nombre) {
        return nombre % 2 == 0;
    }

    public int valeurAbsolue(int nombre) {
        return Math.abs(nombre);
    }
}
