package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de securite pour la classe {@code RequeteurSQL}.
 *
 * <p>Valide la vulnerabilite d'injection SQL par concatenation de chaines
 * et l'efficacite de la protection par parametre prepare.</p>
 *
 * <p>Les tests verifient que :</p>
 * <ul>
 *   <li>Les charges utiles SQL sont presentes dans les requetes vulnerables.</li>
 *   <li>Les requetes securisees utilisent des parametres prepares sans injection possible.</li>
 *   <li>Les vecteurs d'attaque multiples sont correctement geres.</li>
 * </ul>
 */
@DisplayName("OWASP : Tests de securite du RequeteurSQL")
class RequeteurSQLTest {

    /**
     * Instance du requeteur SQL utilisee dans tous les tests.
     */
    private final RequeteurSQL requeteur = new RequeteurSQL();

    /**
     * Tests d'injection SQL : versions vulnerable et securisee.
     */
    @Nested
    @DisplayName("Injection SQL")
    class InjectionSQL {

        /**
         * Verifie que la requete vulnerable contient la charge utile d'injection,
         * prouvant l'absence de filtrage des entrees utilisateur.
         */
        @Test
        @DisplayName("Requete vulnerable : l'injection SQL est possible")
        void requeteVulnerableInjectionPossible() {
            String requete = requeteur.construireRequeteVulnerable("' OR '1'='1' --");
            assertTrue(requete.contains("' OR '1'='1' --"),
                "Preuve de vulnerabilite : la chaine d'injection est dans la requete");
        }

        /**
         * Verifie que la requete securisee utilise un parametre prepare {@code ?}
         * et que les donnees utilisateur ne sont pas presentes dans la requete.
         */
        @Test
        @DisplayName("Requete securisee : pas possible d'injecter — le parametre '?' est utilise")
        void requeteSecuriseePasInjection() {
            String requete = requeteur.construireRequeteSecurisee("' OR '1'='1' --");
            assertTrue(requete.contains("?"),
                "La requete utilise un parametre ?");
            assertFalse(requete.contains("' OR '1'='1"),
                "Les donnees utilisateur NE sont PAS dans la requete");
        }

        /**
         * Test parametre verifiant que chaque vecteur d'injection SQL est
         * present dans la version vulnerable et absent de la version securisee.
         *
         * @param injection la charge utile d'injection SQL a tester
         */
        @ParameterizedTest
        @DisplayName("Detection d'injection sur entrees malveillantes")
        @ValueSource(strings = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "1' UNION SELECT * FROM users --",
            "admin'--",
            "' OR 1=1 --"
        })
        void entreesMalveillantes(String injection) {
            String vuln = requeteur.construireRequeteVulnerable(injection);
            assertTrue(vuln.contains(injection),
                "La version vulnerable injecte directement : " + injection);

            String sec = requeteur.construireRequeteSecurisee(injection);
            assertFalse(sec.contains(injection),
                "La version securisee n'injecte pas : " + injection);
        }
    }
}
