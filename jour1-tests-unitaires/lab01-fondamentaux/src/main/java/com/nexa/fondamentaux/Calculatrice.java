package com.nexa.fondamentaux;

/**
 * <h1>Classe Calculatrice — Code à tester</h1>
 *
 * <p>Cette classe simple contient les opérations arithmétiques de base.
 * Elle sert de cible pour nos premiers tests unitaires.</p>
 *
 * <h2>Pourquoi cette classe est un bon point de départ ?</h2>
 * <ul>
 *   <li>Méthodes <b>pures</b> (sans état) : mêmes entrées → même sortie, faciles à tester</li>
 *   <li>Comportement <b>déterministe</b> : pas de hasard, pas de dépendances externes</li>
 *   <li>Cas d'erreur <b>explicite</b> : division par zéro → exception</li>
 * </ul>
 *
 * <h2>Convention de nommage</h2>
 * <p>La classe à tester est dans {@code src/main/java}, ses tests dans {@code src/test/java}.
 * Le nom du test est souvent {@code <Classe>Test.java} : ici {@code CalculatriceTest.java}.</p>
 */
public class Calculatrice {

    /**
     * Additionne deux entiers.
     *
     * @param a premier opérande
     * @param b second opérande
     * @return la somme a + b
     */
    public int addition(int a, int b) {
        return a + b;
    }

    /**
     * Soustrait deux entiers.
     *
     * @param a premier opérande
     * @param b second opérande
     * @return la différence a - b
     */
    public int soustraction(int a, int b) {
        return a - b;
    }

    /**
     * Multiplie deux entiers.
     *
     * @param a premier opérande
     * @param b second opérande
     * @return le produit a * b
     */
    public int multiplication(int a, int b) {
        return a * b;
    }

    /**
     * Divise deux entiers.
     *
     * @param a dividende
     * @param b diviseur
     * @return le quotient a / b (division entière)
     * @throws ArithmeticException si le diviseur est zéro
     */
    public int division(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Division par zéro interdite");
        }
        return a / b;
    }

    /**
     * Calcule le modulo (reste de la division euclidienne).
     *
     * @param a dividende
     * @param b diviseur
     * @return le reste a % b
     * @throws ArithmeticException si le diviseur est zéro
     */
    public int modulo(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Modulo par zéro interdit");
        }
        return a % b;
    }

    /**
     * Indique si un nombre est pair.
     *
     * @param nombre le nombre à tester
     * @return true si pair, false si impair
     */
    public boolean estPair(int nombre) {
        return nombre % 2 == 0;
    }

    /**
     * Retourne la valeur absolue d'un entier.
     *
     * @param nombre l'entier
     * @return |nombre|, toujours positif ou nul
     */
    public int valeurAbsolue(int nombre) {
        return Math.abs(nombre);
    }
}
