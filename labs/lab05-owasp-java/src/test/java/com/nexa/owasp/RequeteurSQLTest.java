package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP : Tests de sécurité du RequeteurSQL")
class RequeteurSQLTest {

    private final RequeteurSQL requeteur = new RequeteurSQL();

    @Nested
    @DisplayName("Injection SQL")
    class InjectionSQL {

        @Test
        @DisplayName("Requête vulnérable : l'injection SQL est possible")
        void requeteVulnerableInjectionPossible() {
            String requete = requeteur.construireRequeteVulnerable("' OR '1'='1' --");
            assertTrue(requete.contains("' OR '1'='1' --"),
                "Preuve de vulnérabilité : la chaîne d'injection est dans la requête");
        }

        @Test
        @DisplayName("Requête sécurisée : pas possible d'injecter — le paramètre '?' est utilisé")
        void requeteSecuriseePasInjection() {
            String requete = requeteur.construireRequeteSecurisee("' OR '1'='1' --");
            assertTrue(requete.contains("?"),
                "La requête utilise un paramètre ?");
            assertFalse(requete.contains("' OR '1'='1'"),
                "Les données utilisateur NE sont PAS dans la requête");
        }

        @ParameterizedTest
        @DisplayName("Détection d'injection sur entrées malveillantes")
        @ValueSource(strings = {
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "1' UNION SELECT * FROM users --",
            "admin'--",
            "' OR 1=1 --"
        })
        void entréesMalveillantes(String injection) {
            String vuln = requeteur.construireRequeteVulnerable(injection);
            assertTrue(vuln.contains(injection),
                "La version vulnérable injecte directement : " + injection);

            String sec = requeteur.construireRequeteSecurisee(injection);
            assertFalse(sec.contains(injection),
                "La version sécurisée n'injecte pas : " + injection);
        }
    }
}
