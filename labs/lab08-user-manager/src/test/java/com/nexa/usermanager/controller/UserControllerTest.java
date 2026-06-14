package com.nexa.usermanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.usermanager.dto.UserRequest;
import com.nexa.usermanager.dto.UserResponse;
import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.security.JwtUtil;
import com.nexa.usermanager.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(com.nexa.usermanager.config.SecurityConfig.class)
@DisplayName("Tests MVC : UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private UserService service;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private AuthenticationManager authManager;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Dupont", "Jean", "jean@test.com", "pass", User.Role.USER);
        user.setId(1L);
    }

    @Test
    @DisplayName("GET /api/users → 200 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void listerAvecAdmin() throws Exception {
        when(service.listerTous()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Dupont"));
    }

    @Test
    @DisplayName("GET /api/users → 200 avec USER")
    @WithMockUser(roles = "USER")
    void listerAvecUser() throws Exception {
        when(service.listerTous()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users sans authentification → 403")
    void listerSansAuth() throws Exception {
        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/{id} → 200")
    @WithMockUser(roles = "USER")
    void trouverParId() throws Exception {
        when(service.trouverParId(1L)).thenReturn(user);
        mockMvc.perform(get("/api/users/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("jean@test.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} inexistant → 404")
    @WithMockUser(roles = "USER")
    void trouverParIdInexistant() throws Exception {
        when(service.trouverParId(99L))
            .thenThrow(new com.nexa.usermanager.exception.ResourceNotFoundException("Introuvable"));
        mockMvc.perform(get("/api/users/99").with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users → 201 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void creerAvecAdmin() throws Exception {
        UserRequest req = new UserRequest();
        req.setNom("Nouveau");
        req.setPrenom("Utilisateur");
        req.setEmail("nouveau@test.com");
        req.setPassword("password123");
        req.setRole("USER");

        when(service.creer(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nom").value("Dupont"));
    }

    @Test
    @DisplayName("POST /api/users avec USER → 403")
    @WithMockUser(roles = "USER")
    void creerAvecUserInterdit() throws Exception {
        UserRequest req = new UserRequest();
        req.setNom("Nouveau");
        req.setPrenom("Test");
        req.setEmail("test@test.com");
        req.setPassword("password123");
        req.setRole("USER");

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/users avec données invalides → 400")
    @WithMockUser(roles = "ADMIN")
    void creerInvalide() throws Exception {
        UserRequest req = new UserRequest();
        req.setNom("A");
        req.setPrenom("");
        req.setEmail("pas-un-email");
        req.setPassword("court");
        req.setRole("INVALIDE");

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/{id} → 200 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void mettreAJour() throws Exception {
        UserRequest req = new UserRequest();
        req.setNom("Modifié");
        req.setPrenom("Jean");
        req.setEmail("jean@test.com");
        req.setPassword("newpass123");
        req.setRole("ADMIN");

        User updated = new User("Modifié", "Jean", "jean@test.com", "newpass123", User.Role.ADMIN);
        updated.setId(1L);

        when(service.mettreAJour(eq(1L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Modifié"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} → 204 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void supprimer() throws Exception {
        doNothing().when(service).supprimer(1L);
        mockMvc.perform(delete("/api/users/1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} avec USER → 403")
    @WithMockUser(roles = "USER")
    void supprimerInterditUser() throws Exception {
        mockMvc.perform(delete("/api/users/1").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/recherche?nom=dup → 200")
    @WithMockUser(roles = "USER")
    void rechercher() throws Exception {
        when(service.rechercherParNom("dup")).thenReturn(List.of(user));
        mockMvc.perform(get("/api/users/recherche").param("nom", "dup").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Dupont"));
    }

    @Test
    @DisplayName("GET /api/users/actifs → 200")
    @WithMockUser(roles = "USER")
    void actifs() throws Exception {
        when(service.listerActifs()).thenReturn(List.of(user));
        mockMvc.perform(get("/api/users/actifs").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].actif").value(true));
    }
}
