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

/**
 * Tests d'integration MVC pour le controleur {@link UserController}.
 *
 * <p>Cette classe teste tous les endpoints REST de l'API avec MockMvc.
 * Elle vérifié :</p>
 * <ul>
 *   <li>Les codes de statut HTTP retournes (200, 201, 204, 400, 403, 404).</li>
 *   <li>Le controle d'accès par role (ADMIN vs USER vs anonyme).</li>
 *   <li>La validation des donnees d'entree.</li>
 *   <li>Le contenu JSON des reponses (via JsonPath).</li>
 * </ul>
 *
 * <p>Les dépendances (service, JWT, auth) sont mockees avec {@code @MockBean}
 * pour isoler la couche controleur.</p>
 */
@WebMvcTest
@Import(com.nexa.usermanager.config.SecurityConfig.class)
@DisplayName("Tests MVC : UserController")
class UserControllerTest {

    /** MockMvc pour simuler les requetes HTTP. */
    @Autowired private MockMvc mockMvc;

    /** ObjectMapper pour serialiser/deserialiser le JSON. */
    @Autowired private ObjectMapper mapper;

    /** Mock du service utilisateur. */
    @MockBean private UserService service;

    /** Mock de l'utilitaire JWT. */
    @MockBean private JwtUtil jwtUtil;

    /** Mock du gestionnaire d'authentification. */
    @MockBean private AuthenticationManager authManager;

    /** Mock du service de details utilisateur pour Spring Security. */
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    /** Utilisateur de test reinitialise avant chaque test. */
    private User user;

    /**
     * Initialise un utilisateur de test avant chaque cas de test.
     */
    @BeforeEach
    void setUp() {
        user = new User("Dupont", "Jean", "jean@test.com", "pass", User.Role.USER);
        user.setId(1L);
    }

    /**
     * Verifie qu'un ADMIN peut lister tous les utilisateurs (HTTP 200).
     */
    @Test
    @DisplayName("GET /api/users -> 200 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void listerAvecAdmin() throws Exception {
        when(service.listerTous()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Dupont"));
    }

    /**
     * Verifie qu'un USER peut lister tous les utilisateurs (HTTP 200).
     */
    @Test
    @DisplayName("GET /api/users -> 200 avec USER")
    @WithMockUser(roles = "USER")
    void listerAvecUser() throws Exception {
        when(service.listerTous()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isOk());
    }

    /**
     * Verifie qu'un utilisateur non authentifie reçoit un 403 Forbidden.
     */
    @Test
    @DisplayName("GET /api/users sans authentification -> 403")
    void listerSansAuth() throws Exception {
        mockMvc.perform(get("/api/users").with(csrf()))
            .andExpect(status().isForbidden());
    }

    /**
     * Verifie la recherche d'un utilisateur par ID (HTTP 200).
     */
    @Test
    @DisplayName("GET /api/users/{id} -> 200")
    @WithMockUser(roles = "USER")
    void trouverParId() throws Exception {
        when(service.trouverParId(1L)).thenReturn(user);
        mockMvc.perform(get("/api/users/1").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("jean@test.com"));
    }

    /**
     * Verifie le comportement quand un ID inexistant est demande (HTTP 404).
     */
    @Test
    @DisplayName("GET /api/users/{id} inexistant -> 404")
    @WithMockUser(roles = "USER")
    void trouverParIdInexistant() throws Exception {
        when(service.trouverParId(99L))
            .thenThrow(new com.nexa.usermanager.exception.ResourceNotFoundException("Introuvable"));
        mockMvc.perform(get("/api/users/99").with(csrf()))
            .andExpect(status().isNotFound());
    }

    /**
     * Verifie qu'un ADMIN peut creer un utilisateur (HTTP 201 Created).
     */
    @Test
    @DisplayName("POST /api/users -> 201 avec ADMIN")
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

    /**
     * Verifie qu'un USER ne peut PAS creer d'utilisateur (HTTP 403 Forbidden).
     */
    @Test
    @DisplayName("POST /api/users avec USER -> 403")
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

    /**
     * Verifie que la validation des donnees rejette les champs invalides (HTTP 400).
     */
    @Test
    @DisplayName("POST /api/users avec donnees invalides -> 400")
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

    /**
     * Verifie qu'un ADMIN peut mettre a jour un utilisateur (HTTP 200).
     */
    @Test
    @DisplayName("PUT /api/users/{id} -> 200 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void mettreAJour() throws Exception {
        UserRequest req = new UserRequest();
        req.setNom("Modifie");
        req.setPrenom("Jean");
        req.setEmail("jean@test.com");
        req.setPassword("newpass123");
        req.setRole("ADMIN");

        User updated = new User("Modifie", "Jean", "jean@test.com", "newpass123", User.Role.ADMIN);
        updated.setId(1L);

        when(service.mettreAJour(eq(1L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Modifie"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /**
     * Verifie qu'un ADMIN peut supprimer un utilisateur (HTTP 204 No Content).
     */
    @Test
    @DisplayName("DELETE /api/users/{id} -> 204 avec ADMIN")
    @WithMockUser(roles = "ADMIN")
    void supprimer() throws Exception {
        doNothing().when(service).supprimer(1L);
        mockMvc.perform(delete("/api/users/1").with(csrf()))
            .andExpect(status().isNoContent());
    }

    /**
     * Verifie qu'un USER ne peut PAS supprimer d'utilisateur (HTTP 403).
     */
    @Test
    @DisplayName("DELETE /api/users/{id} avec USER -> 403")
    @WithMockUser(roles = "USER")
    void supprimerInterditUser() throws Exception {
        mockMvc.perform(delete("/api/users/1").with(csrf()))
            .andExpect(status().isForbidden());
    }

    /**
     * Verifie la recherche d'utilisateurs par nom (HTTP 200).
     */
    @Test
    @DisplayName("GET /api/users/recherche?nom=dup -> 200")
    @WithMockUser(roles = "USER")
    void rechercher() throws Exception {
        when(service.rechercherParNom("dup")).thenReturn(List.of(user));
        mockMvc.perform(get("/api/users/recherche").param("nom", "dup").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Dupont"));
    }

    /**
     * Verifie le listing des utilisateurs actifs (HTTP 200).
     */
    @Test
    @DisplayName("GET /api/users/actifs -> 200")
    @WithMockUser(roles = "USER")
    void actifs() throws Exception {
        when(service.listerActifs()).thenReturn(List.of(user));
        mockMvc.perform(get("/api/users/actifs").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].actif").value(true));
    }
}
