package com.nexa.fondamentaux;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de la classe Calculatrice")
class CalculatriceTest {

    private final Calculatrice calc = new Calculatrice();

    @Test
    @DisplayName("Addition de deux nombres positifs")
    void additionDeuxPositifs() {
        assertEquals(5, calc.addition(2, 3),
            "2 + 3 devrait donner 5");
    }

    @Test
    @DisplayName("Addition avec un nombre négatif")
    void additionAvecNegatif() {
        assertEquals(1, calc.addition(3, -2));
        assertEquals(-5, calc.addition(-2, -3));
    }

    @Test
    @DisplayName("Addition avec zéro (élément neutre)")
    void additionAvecZero() {
        assertEquals(7, calc.addition(7, 0),
            "a + 0 devrait toujours donner a");
        assertEquals(7, calc.addition(0, 7),
            "0 + a devrait toujours donner a");
    }

    @Test
    @DisplayName("Soustraction simple")
    void soustractionSimple() {
        assertEquals(3, calc.soustraction(7, 4));
    }

    @Test
    @DisplayName("Soustraction donnant un résultat négatif")
    void soustractionResultatNegatif() {
        assertEquals(-3, calc.soustraction(2, 5));
    }

    @Test
    @DisplayName("Multiplication simple")
    void multiplicationSimple() {
        assertEquals(12, calc.multiplication(3, 4));
    }

    @Test
    @DisplayName("Multiplication par zéro (élément absorbant)")
    void multiplicationParZero() {
        assertEquals(0, calc.multiplication(5, 0));
        assertEquals(0, calc.multiplication(0, 5));
    }

    @Test
    @DisplayName("Multiplication par un (élément neutre)")
    void multiplicationParUn() {
        assertEquals(7, calc.multiplication(7, 1));
    }

    @Test
    @DisplayName("Division simple")
    void divisionSimple() {
        assertEquals(3, calc.division(9, 3));
    }

    @Test
    @DisplayName("Division avec reste (troncature entière)")
    void divisionAvecReste() {
        assertEquals(3, calc.division(10, 3),
            "10 / 3 = 3 (division entière, le reste est ignoré)");
    }

    @Test
    @DisplayName("Division par zéro doit lever ArithmeticException")
    void divisionParZero() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> calc.division(10, 0),
            "La division par zéro doit lever une ArithmeticException"
        );
        assertTrue(exception.getMessage().contains("Division par zéro"));
    }

    @Test
    @DisplayName("Modulo simple")
    void moduloSimple() {
        assertEquals(1, calc.modulo(10, 3));
    }

    @Test
    @DisplayName("Modulo sans reste")
    void moduloSansReste() {
        assertEquals(0, calc.modulo(9, 3));
    }

    @Test
    @DisplayName("Modulo par zéro doit lever exception")
    void moduloParZero() {
        assertThrows(ArithmeticException.class,
            () -> calc.modulo(5, 0));
    }

    @Test
    @DisplayName("Nombre pair")
    void nombrePair() {
        assertTrue(calc.estPair(2), "2 est pair");
        assertTrue(calc.estPair(0), "0 est pair");
        assertTrue(calc.estPair(-4), "-4 est pair");
    }

    @Test
    @DisplayName("Nombre impair")
    void nombreImpair() {
        assertFalse(calc.estPair(1), "1 est impair");
        assertFalse(calc.estPair(-3), "-3 est impair");
    }

    @Test
    @DisplayName("Vérifications groupées avec assertAll")
    void testGroupeAvecAssertAll() {
        assertAll("Vérifications groupées de la calculatrice",
            () -> assertEquals(5, calc.addition(2, 3), "addition"),
            () -> assertEquals(6, calc.multiplication(2, 3), "multiplication"),
            () -> assertEquals(-1, calc.soustraction(2, 3), "soustraction"),
            () -> assertEquals(0, calc.division(0, 5), "division de 0"),
            () -> assertTrue(calc.estPair(10), "parité de 10"),
            () -> assertEquals(5, calc.valeurAbsolue(-5), "valeur absolue")
        );
    }

    @Test
    @DisplayName("Valeur absolue d'un nombre positif (ne change pas)")
    void valeurAbsoluePositif() {
        assertEquals(5, calc.valeurAbsolue(5));
    }

    @Test
    @DisplayName("Valeur absolue d'un nombre négatif (devient positif)")
    void valeurAbsolueNegatif() {
        assertEquals(5, calc.valeurAbsolue(-5));
    }

    @Test
    @DisplayName("Valeur absolue de zéro")
    void valeurAbsolueZero() {
        assertEquals(0, calc.valeurAbsolue(0));
    }

    @Test
    @DisplayName("Démonstration : messages d'erreur personnalisés")
    void testAvecMessagePersonnalise() {
        assertEquals(4, calc.addition(2, 2),
            "Ce message s'affiche si 2+2 ≠ 4");

        assertTrue(calc.estPair(2),
            () -> "Message paresseux construit uniquement en cas d'échec pour 2");
    }

    @Test
    @DisplayName("assertNotNull : l'objet calculatrice existe")
    void calculatriceNonNulle() {
        assertNotNull(calc, "L'instance de Calculatrice ne doit pas être null");
    }

    @Test
    @DisplayName("assertNull : test de nullité")
    void testNull() {
        String chaine = null;
        assertNull(chaine);
    }

    @Test
    @DisplayName("assertArrayEquals : comparaison de tableaux")
    void testTableaux() {
        int[] attendu = {2, 4, 6, 8};
        int[] obtenu = {
            calc.multiplication(2, 1),
            calc.multiplication(2, 2),
            calc.multiplication(2, 3),
            calc.multiplication(2, 4)
        };
        assertArrayEquals(attendu, obtenu, "Table multipliée par 2");
    }
}
