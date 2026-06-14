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
 * <h1>Tests de sécurité avec authentification JWT réelle</h1>
 *
 * <h2>{@code @SpringBootTest(webEnvironment = RANDOM_PORT)}</h2>
 * <p>
 * Charge le contexte Spring COMPLET (tous les beans, sécurité, base de données).
 * C'est un vrai test d'intégration.
 * </p>
 *
 * <h2>JWT dans le header Authorization</h2>
 * <p>
 * On génère un vrai token JWT avec {@code JwtUtil} et on le passe
 * dans le header {@code Authorization: Bearer <token>} pour simuler
 * un utilisateur authentifié.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Tests de sécurité — intégration")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("Accès public")
    class AccesPublic {

        @Test
        @DisplayName("Sans token : l'endpoint public est accessible")
        void sansTokenPublicAccessible() throws Exception {
            mockMvc.perform(get("/api/produits/public"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Avec token USER : l'endpoint public est accessible")
        void avecTokenUserPublicAccessible() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/public")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Contrôle d'accès par rôle")
    class ControleAcces {

        @Test
        @DisplayName("Token ADMIN peut accéder à l'espace admin")
        void adminAccedeAdmin() throws Exception {
            String token = jwtUtil.genererToken("admin", "ADMIN");
            mockMvc.perform(get("/api/produits/admin")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Token USER ne peut PAS accéder à l'espace admin → 403")
        void userNePeutPasAccederAdmin() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/admin")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Token USER peut accéder à l'espace user")
        void userAccedeUser() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Token ADMIN peut accéder à l'espace user (hasAnyRole)")
        void adminAccedeUser() throws Exception {
            String token = jwtUtil.genererToken("admin", "ADMIN");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Authentification")
    class Authentification {

        @Test
        @DisplayName("Sans token : endpoint sécurisé → 403 Forbidden")
        void sansTokenEndpointSecurise() throws Exception {
            mockMvc.perform(get("/api/produits/admin"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Token invalide → 403 Forbidden")
        void tokenInvalide() throws Exception {
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", "Bearer token_invalide"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Header Authorization absent → 403")
        void headerAuthorizationAbsent() throws Exception {
            mockMvc.perform(get("/api/produits/user"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Header Authorization format incorrect → 403")
        void headerFormatIncorrect() throws Exception {
            String token = jwtUtil.genererToken("user", "USER");
            mockMvc.perform(get("/api/produits/user")
                    .header("Authorization", token)) // Sans "Bearer "
                .andExpect(status().isForbidden());
        }
    }
}
