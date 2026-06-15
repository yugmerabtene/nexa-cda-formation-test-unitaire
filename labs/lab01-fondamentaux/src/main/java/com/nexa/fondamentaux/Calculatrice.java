package com.nexa.fondamentaux;

/**
 * Classe Calculatrice : implementation d'une calculatrice simple.
 *
 * Cette classe sert de support pour les premiers tests unitaires.
 * Chaque méthode est concue pour etre testable de maniere isolee.
 *
 * Les tests correspondants sont dans CalculatriceTest.java.
 *
 * @see CalculatriceTest
 */
public class Calculatrice {

    /**
     * Additionne deux entiers.
     *
     * @param a premier operande (int)
     * @param b deuxieme operande (int)
     * @return la somme a + b
     *
     * Exemple : addition(2, 3) -> 5
     */
    public int addition(int a, int b) {
        return a + b;
    }

    /**
     * Soustrait deux entiers.
     *
     * @param a premier operande (int)
     * @param b deuxieme operande a soustraire (int)
     * @return la difference a - b
     *
     * Exemple : soustraction(7, 4) -> 3
     */
    public int soustraction(int a, int b) {
        return a - b;
    }

    /**
     * Multiplie deux entiers.
     *
     * @param a premier facteur (int)
     * @param b deuxieme facteur (int)
     * @return le produit a * b
     *
     * Exemple : multiplication(3, 4) -> 12
     */
    public int multiplication(int a, int b) {
        return a * b;
    }

    /**
     * Divise a par b (division entiere).
     *
     * Leve une ArithmeticException si b vaut 0,
     * car la division par zero est indefinie en mathematiques.
     *
     * @param a dividende (int)
     * @param b diviseur (int), ne doit pas etre zero
     * @return le quotient entier a / b
     * @throws ArithmeticException si b == 0
     *
     * Exemple : division(9, 3) -> 3
     */
    public int division(int a, int b) {
        // Verification du diviseur avant d'effectuer la division
        if (b == 0) {
            throw new ArithmeticException("Division par zero interdite");
        }
        // Division entiere (les decimales sont tronquees)
        return a / b;
    }

    /**
     * Calcule le reste de la division entiere (modulo).
     *
     * Leve une ArithmeticException si b vaut 0.
     *
     * @param a dividende (int)
     * @param b diviseur (int), ne doit pas etre zero
     * @return le reste de la division a % b
     * @throws ArithmeticException si b == 0
     *
     * Exemple : modulo(10, 3) -> 1 (car 10 = 3*3 + 1)
     */
    public int modulo(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Modulo par zero interdit");
        }
        return a % b;
    }

    /**
     * Verifie si un nombre est pair.
     *
     * Un nombre est pair si le reste de sa division par 2 vaut 0.
     * Zero est considere comme pair (0 % 2 == 0).
     * Les nombres negatifs suivent la même regle (-4 % 2 == 0).
     *
     * @param nombre le nombre a tester (int)
     * @return true si le nombre est pair, false s'il est impair
     *
     * Exemples :
     * - estPair(2) -> true
     * - estPair(1) -> false
     * - estPair(0) -> true
     * - estPair(-4) -> true
     */
    public boolean estPair(int nombre) {
        // L'operateur modulo (%) donne le reste de la division par 2
        return nombre % 2 == 0;
    }

    /**
     * Retourne la valeur absolue du nombre fourni.
     *
     * La valeur absolue est la distance d'un nombre a zero, toujours positive.
     * Math.abs() gère les cas positifs, negatifs, et zero.
     *
     * @param nombre le nombre dont on veut la valeur absolue (int)
     * @return la valeur absolue du nombre (toujours >= 0)
     *
     * Exemples :
     * - valeurAbsolue(5) -> 5
     * - valeurAbsolue(-5) -> 5
     * - valeurAbsolue(0) -> 0
     */
    public int valeurAbsolue(int nombre) {
        // Delegation a la méthode standard de Java
        return Math.abs(nombre);
    }
}
