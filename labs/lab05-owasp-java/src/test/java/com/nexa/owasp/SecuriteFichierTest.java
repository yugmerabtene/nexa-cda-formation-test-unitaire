package com.nexa.owasp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de securite pour la classe {@link SecuriteFichier}.
 *
 * <p>Valide la protection contre les attaques de Path Traversal (ou Directory
 * Traversal) qui permettent a un attaquant d'acceder a des fichiers situes
 * hors du repertoire autorise.</p>
 *
 * <p>Les tests couvrent :</p>
 * <ul>
 *   <li>Version vulnerable : le {@code ../} est concatene sans verification.</li>
 *   <li>Version securisee : les caracteres interdits sont bloques, le chemin
 *       est normalise et verifie.</li>
 *   <li>Detection : identification des motifs de Path Traversal.</li>
 *   <li>Cas limites : chemins absolus, backslash Windows, entrees nulles.</li>
 * </ul>
 */
@DisplayName("OWASP : Tests de securite fichier (Path Traversal)")
class SecuriteFichierTest {

    /**
     * Instance de securite fichier utilisee dans tous les tests.
     */
    private final SecuriteFichier securite = new SecuriteFichier();

    /**
     * Tests de Path Traversal : construction de chemins vulnerable et securisee.
     */
    @Nested
    @DisplayName("Path Traversal")
    class PathTraversal {

        /**
         * Verifie que la version vulnerable accepte le motif {@code ../}
         * sans verification, permettant de remonter dans l'arborescence.
         */
        @Test
        @DisplayName("Chemin vulnerable : le path traversal est possible")
        void cheminVulnerablePathTraversalPossible() {
            String chemin = securite.construireCheminVulnerable("../../etc/passwd");
            assertEquals("/var/data/../../etc/passwd", chemin,
                "Preuve : le ../ est concatene tel quel");
        }

        /**
         * Verifie que la version securisee bloque les motifs {@code ../}
         * en lancant une exception.
         */
        @Test
        @DisplayName("Chemin securise : les ../ sont bloques")
        void cheminSecurisePathTraversalBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("../../etc/passwd"));
        }

        /**
         * Verifie que les chemins absolus (debutant par {@code /}) sont
         * bloques par la version securisee.
         */
        @Test
        @DisplayName("Chemin securise : les chemins absolus sont bloques")
        void cheminAbsoluBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("/etc/passwd"));
        }

        /**
         * Verifie qu'un nom de fichier valide sans caracteres speciaux
         * est accepte et que le chemin reste dans le repertoire autorise.
         */
        @Test
        @DisplayName("Chemin securise : nom de fichier valide accepte")
        void nomFichierValideAccepte() {
            String chemin = securite.construireCheminSecurise("rapport.pdf");
            assertTrue(chemin.endsWith("rapport.pdf"),
                "Le nom de fichier valide est accepte");
            assertTrue(chemin.startsWith("/var/data/"),
                "Le chemin est dans le repertoire autorise");
        }

        /**
         * Verifie que les backslashes Windows ({@code \}) sont egalement
         * bloques, couvrant les attaques multi-plateformes.
         */
        @Test
        @DisplayName("Chemin securise : backslash Windows aussi bloque")
        void backslashWindowsBloque() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise("..\\..\\windows\\system32"));
        }

        /**
         * Verifie qu'une entree nulle est rejetee avec une exception
         * appropriee.
         */
        @Test
        @DisplayName("Chemin securise : null rejete")
        void nullRejete() {
            assertThrows(IllegalArgumentException.class,
                () -> securite.construireCheminSecurise(null));
        }
    }

    /**
     * Tests de detection des tentatives de Path Traversal.
     *
     * <p>Verifient la fonction {@code estTentativePathTraversal} qui
     * identifie les motifs d'attaque sans tenter de construire le chemin.</p>
     */
    @Nested
    @DisplayName("Detection de tentatives")
    class DetectionTentatives {

        /**
         * Verifie que les motifs classiques de Path Traversal sont correctement
         * detectes (remontee de repertoire, chemin absolu, octet nul).
         *
         * @param input la chaine a tester pour la detection de Path Traversal
         */
        @ParameterizedTest
        @DisplayName("Detection positive de path traversal")
        @ValueSource(strings = {
            "../../etc/passwd",
            "..\\..\\windows\\system32",
            "/etc/passwd",
            "foo/../bar",
            "file.txt\0.jpg"
        })
        void detectionPositive(String input) {
            assertTrue(securite.estTentativePathTraversal(input),
                "Devrait detecter une tentative : " + input);
        }

        /**
         * Verifie que des noms de fichiers valides ne declenchent pas de
         * faux positifs dans la detection de Path Traversal.
         */
        @Test
        @DisplayName("Detection negative : fichiers valides")
        void detectionNegative() {
            assertFalse(securite.estTentativePathTraversal("rapport.pdf"));
            assertFalse(securite.estTentativePathTraversal("image.png"));
            assertFalse(securite.estTentativePathTraversal(null));
        }
    }
}
