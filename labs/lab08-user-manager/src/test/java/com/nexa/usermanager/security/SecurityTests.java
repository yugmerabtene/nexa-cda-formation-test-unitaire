package com.nexa.usermanager.security;

import com.nexa.usermanager.config.SecurityConfig;
import com.nexa.usermanager.controller.UserController;
import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration pour la configuration de sécurité Spring Security.
 *
 * <p>Cette classe vérifié le controle d'accès aux endpoints en fonction
 * des roles et de l'état d'authentification :</p>
 * <ul>
 *   <li>Les endpoints publics ({@code /api/auth/**}, {@code /actuator/health})
 *       sont accessibles sans authentification.</li>
 *   <li>Le role ADMIN peut effectuer toutes les operations (CRUD).</li>
 *   <li>Le role USER ne peut qu'effectuer des lectures (GET).</li>
 *   <li>Les utilisateurs anonymes sont refuses sur les endpoints proteges (403).</li>
 * </ul>
 */
@WebMvcTest
@Import(SecurityConfig.class)
@DisplayName("Tests de sécurité : Controle d'accès")
class SecurityTests {

    /** MockMvc pour simuler les requetes HTTP. */
    @Autowired private MockMvc mockMvc;

    /** Mock du service utilisateur. */
    @MockBean private UserService userService;

    /** Mock du controleur utilisateur. */
    @MockBean private UserController userController;

    /** Mock de l'utilitaire JWT. */
    @MockBean private JwtUtil jwtUtil;

    /** Mock du service de details utilisateur. */
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    /**
     * Classe interne regroupant les tests des endpoints publics
     * (accessibles sans authentification).
     */
    @Nested
    @DisplayName("Endpoints publics (sans authentification)")
    class EndpointsPublics {

        /**
         * Verifie que le endpoint d'authentification est accessible sans auth.
         * Retourne 401 car les identifiants de test sont invalides.
         */
        @Test
        @DisplayName("/api/auth/** -> accessible sans auth")
        @WithAnonymousUser
        void authAccessible() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized());
        }

        /**
         * Verifie que le health check Actuator est accessible sans authentification.
         */
        @Test
        @DisplayName("/actuator/health -> accessible sans auth")
        @WithAnonymousUser
        void healthAccessible() throws Exception {
            mockMvc.perform(get("/actuator/health").with(csrf()))
                .andExpect(status().isOk());
        }
    }

    /**
     * Classe interne regroupant les tests de controle d'accès par role.
     */
    @Nested
    @DisplayName("Controle d'accès par role")
    class ControleParRole {

        /**
         * Verifie que le role ADMIN peut creer un utilisateur (HTTP 201).
         */
        @Test
        @DisplayName("ADMIN peut faire POST /api/users")
        @WithMockUser(roles = "ADMIN")
        void adminPost() throws Exception {
            mockMvc.perform(post("/api/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nom\":\"Test\",\"prenom\":\"T\",\"email\":\"t@t.com\",\"password\":\"12345678\",\"role\":\"USER\"}"))
                .andExpect(status().isCreated());
        }

        /**
         * Verifie que le role USER ne peut PAS creer d'utilisateur (HTTP 403).
         */
        @Test
        @DisplayName("USER ne peut PAS faire POST /api/users")
        @WithMockUser(roles = "USER")
        void userPostInterdit() throws Exception {
            mockMvc.perform(post("/api/users")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie que le role ADMIN peut modifier un utilisateur (HTTP 200).
         */
        @Test
        @DisplayName("ADMIN peut faire PUT /api/users/{id}")
        @WithMockUser(roles = "ADMIN")
        void adminPut() throws Exception {
            mockMvc.perform(put("/api/users/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nom\":\"Test\",\"prenom\":\"T\",\"email\":\"t@t.com\",\"password\":\"12345678\",\"role\":\"USER\"}"))
                .andExpect(status().isOk());
        }

        /**
         * Verifie que le role USER ne peut PAS modifier d'utilisateur (HTTP 403).
         */
        @Test
        @DisplayName("USER ne peut PAS faire PUT /api/users/{id}")
        @WithMockUser(roles = "USER")
        void userPutInterdit() throws Exception {
            mockMvc.perform(put("/api/users/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie que le role ADMIN peut supprimer un utilisateur (HTTP 204).
         */
        @Test
        @DisplayName("ADMIN peut faire DELETE /api/users/{id}")
        @WithMockUser(roles = "ADMIN")
        void adminDelete() throws Exception {
            mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isNoContent());
        }

        /**
         * Verifie que le role USER ne peut PAS supprimer d'utilisateur (HTTP 403).
         */
        @Test
        @DisplayName("USER ne peut PAS faire DELETE /api/users/{id}")
        @WithMockUser(roles = "USER")
        void userDeleteInterdit() throws Exception {
            mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isForbidden());
        }

        /**
         * Verifie que le role USER peut lister les utilisateurs (HTTP 200).
         */
        @Test
        @DisplayName("USER peut faire GET /api/users")
        @WithMockUser(roles = "USER")
        void userGet() throws Exception {
            mockMvc.perform(get("/api/users").with(csrf()))
                .andExpect(status().isOk());
        }

        /**
         * Verifie qu'un utilisateur anonyme est refuse sur GET /api/users (HTTP 403).
         */
        @Test
        @DisplayName("Sans role, GET /api/users -> 403")
        @WithAnonymousUser
        void sansRoleGetInterdit() throws Exception {
            mockMvc.perform(get("/api/users").with(csrf()))
                .andExpect(status().isForbidden());
        }
    }
}
