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

@WebMvcTest
@Import(SecurityConfig.class)
@DisplayName("Tests de sécurité : Contrôle d'accès")
class SecurityTests {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    @MockBean private UserController userController;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Nested
    @DisplayName("Endpoints publics (sans authentification)")
    class EndpointsPublics {

        @Test
        @DisplayName("/api/auth/** → accessible sans auth")
        @WithAnonymousUser
        void authAccessible() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("/actuator/health → accessible sans auth")
        @WithAnonymousUser
        void healthAccessible() throws Exception {
            mockMvc.perform(get("/actuator/health").with(csrf()))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Contrôle d'accès par rôle")
    class ControleParRole {

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

        @Test
        @DisplayName("ADMIN peut faire DELETE /api/users/{id}")
        @WithMockUser(roles = "ADMIN")
        void adminDelete() throws Exception {
            mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("USER ne peut PAS faire DELETE /api/users/{id}")
        @WithMockUser(roles = "USER")
        void userDeleteInterdit() throws Exception {
            mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER peut faire GET /api/users")
        @WithMockUser(roles = "USER")
        void userGet() throws Exception {
            mockMvc.perform(get("/api/users").with(csrf()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Sans rôle, GET /api/users → 403")
        @WithAnonymousUser
        void sansRoleGetInterdit() throws Exception {
            mockMvc.perform(get("/api/users").with(csrf()))
                .andExpect(status().isForbidden());
        }
    }
}
