package com.nexa.secu.controller;

import com.nexa.secu.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration de la couche de securite Spring Security + JWT.
 * <p>
 * Cette classe utilise {@link MockMvc} pour simuler des requetes HTTP
 * et verifier que les regles de controle d'acces sont correctement appliquees.
 * Le contexte Spring complet est charge (y compris H2 et les utilisateurs
 * par defaut initialises via {@code CommandLineRunner}).
 * <p>
 * Les tests couvrent trois scenarios :
 * <ul>
 *   <li>Acces public : endpoints accessibles sans authentification</li>
 *   <li>Controle d'acces par role : verification des roles ADMIN et USER</li>
 *   <li>Authentification : rejet des tokens invalides, absents ou mal formates</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Tests de sécurité — intégration")
class SecurityIntegrationTest {

    /** Simulateur de requetes HTTP pour les tests d'integration. */
    @Autowired
    private MockMvc mockMvc;

    /** Utilitaire JWT pour generer des tokens de test. */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Tests verifiant que les endpoints marques comme publics sont accessibles
     * avec ou sans token d'authentification.
     */
    @Nested
    @DisplayName("Accès public")
    class AccesPublic {

        /**
         * Verifie que l'endpoint {@code /api/produits/public} retourne 200
         * meme sans header Authorization.
         */
        @Test
        @DisplayName("Sans token : l'endpoint public est accessible")
        void sansTokenPublicAccessible() throws Exception {
            mockMvc.perform(get("/api/produits/public"))
                .andExpect(status().isOk());
        }

        /**
         * Verifie que l'endpoint public est egalement accessible avec un token USER.
         */
        @Test
        @DisplayName("Avec token USER : l'endpoint public est accessible")
        void avecTokenUserPublicAccessible() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/public")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    /**
     * Tests verifiant que le controle d'acces par role fonctionne correctement :
     * ADMIN accede a tout, USER est limite aux endpoints autorises.
     */
    @Nested
    @DisplayName("Contrôle d'accès par rôle")
    class ControleAcces {

        /**
         * Verifie qu'un token avec le role ADMIN peut acceder a l'espace admin (200 OK).
         */
        @Test
        @DisplayName("Token ADMIN peut accéder à l'espace admin")
        void adminAccedeAdmin() throws Exception {
            String token = jwtUtil.genererToken("admin", "ADMIN");
            mockMvc.perform(get("/api/produits/admin")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }

        /**
         * Verifie qu'un token avec le role USER recoit un 403 Forbidden
         * en tentant d'acceder a l'espace admin.
         */
        @Test
        @DisplayName("Token USER ne peut PAS accéder à l'espace admin → 403")
        void userNePeutPasAccederAdmin() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/admin")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie qu'un token avec le role USER peut acceder a l'espace user (200 OK).
         */
        @Test
        @DisplayName("Token USER peut accéder à l'espace user")
        void userAccedeUser() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }

        /**
         * Verifie qu'un token avec le role ADMIN peut acceder a l'espace user
         * grace a l'annotation {@code @PreAuthorize("hasAnyRole('ADMIN','USER')")}.
         */
        @Test
        @DisplayName("Token ADMIN peut accéder à l'espace user (hasAnyRole)")
        void adminAccedeUser() throws Exception {
            String token = jwtUtil.genererToken("admin", "ADMIN");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    /**
     * Tests verifiant le comportement de l'application face a des requetes
     * non authentifiees ou avec des tokens invalides.
     */
    @Nested
    @DisplayName("Authentification")
    class Authentification {

        /**
         * Verifie qu'un endpoint securise retourne 403 quand aucun token
         * n'est fourni.
         */
        @Test
        @DisplayName("Sans token : endpoint sécurisé → 403 Forbidden")
        void sansTokenEndpointSecurise() throws Exception {
            mockMvc.perform(get("/api/produits/admin"))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie qu'un token JWT invalide (signature incorrecte) entraine un 403.
         */
        @Test
        @DisplayName("Token invalide → 403 Forbidden")
        void tokenInvalide() throws Exception {
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer token_invalide"))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie que l'absence totale du header Authorization entraine un 403
         * sur un endpoint securise.
         */
        @Test
        @DisplayName("Header Authorization absent → 403")
        void headerAuthorizationAbsent() throws Exception {
            mockMvc.perform(get("/api/produits/user"))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie que l'envoi d'un token sans le prefixe "Bearer "
         * (format incorrect) entraine un 403.
         */
        @Test
        @DisplayName("Header Authorization format incorrect → 403")
        void headerFormatIncorrect() throws Exception {
            // Token sans le prefixe "Bearer "
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", token))

                .andExpect(status().isForbidden());
        }
    }
}
