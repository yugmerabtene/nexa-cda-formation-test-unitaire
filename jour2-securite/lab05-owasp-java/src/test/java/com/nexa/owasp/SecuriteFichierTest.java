package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OWASP : Tests de sécurité fichier (Path Traversal)")
class SecuriteFichierTest {

    private final SecuriteFichier securite = new SecuriteFichier();

    @Nested
    @DisplayName("Path Traversal")
    class PathTraversal {

        @Test
        @DisplayName("Chemin vulnérable : le path traversal est possible")
        void cheminVulnerablePathTraversalPossible() {
            String chemin = securite.construireCheminVulnerable("../../etc/passwd");
            assertEquals("/var/data/../../etc/passwd", chemin,
                "Preuve : le ../ est concaténé tel quel");
        }

        @Test
        @DisplayName("Chemin sécurisé : les ../ sont bloqués")
        void cheminSecurisePathTraversalBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("../../etc/passwd"));
        }

        @Test
        @DisplayName("Chemin sécurisé : les chemins absolus sont bloqués")
        void cheminAbsoluBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("/etc/passwd"));
        }

        @Test
        @DisplayName("Chemin sécurisé : nom de fichier valide accepté")
        void nomFichierValideAccepte() {
            String chemin = securite.construireCheminSecurise("rapport.pdf");
            assertTrue(chemin.endsWith("rapport.pdf"),
                "Le nom de fichier valide est accepté");
            assertTrue(chemin.startsWith("/var/data/"),
                "Le chemin est dans le répertoire autorisé");
        }

        @Test
        @DisplayName("Chemin sécurisé : backslash Windows aussi bloqué")
        void backslashWindowsBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("..\\..\\windows\\system32"));
        }

        @Test
        @DisplayName("Chemin sécurisé : null rejeté")
        void nullRejete() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise(null));
        }
    }

    @Nested
    @DisplayName("Détection de tentatives")
    class DetectionTentatives {

        @ParameterizedTest
        @DisplayName("Détection positive de path traversal")
        @ValueSource(strings = {
            "../../etc/passwd",
            "..\\..\\windows\\system32",
            "/etc/passwd",
            "foo/../bar",
            "file.txt\0.jpg"
        })
        void detectionPositive(String input) {
            assertTrue(securite.estTentativePathTraversal(input),
                "Devrait détecter une tentative : " + input);
        }

        @Test
        @DisplayName("Détection négative : fichiers valides")
        void detectionNegative() {
            assertFalse(securite.estTentativePathTraversal("rapport.pdf"));
            assertFalse(securite.estTentativePathTraversal("image.png"));
            assertFalse(securite.estTentativePathTraversal(null));
        }
    }
}
