package com.nexa.springintro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexa.springintro.model.Produit;
import com.nexa.springintro.service.ProduitService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration de la couche web avec {@code @WebMvcTest}.
 * Charge uniquement le controleur et simule le service avec {@code @MockBean}.
 */
@WebMvcTest(ProduitController.class)
@DisplayName("Tests MVC du ProduitController")
class ProduitControllerTest {

    /**
     * Objet principal pour simuler des appels HTTP et verifier les reponses.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Mapper Jackson pour serialiser/deserialiser les objets en JSON.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Service simule : remplace le vrai service dans le contexte Spring.
     */
    @MockBean
    private ProduitService produitService;

    /**
     * Produit de test reinitialise avant chaque test.
     */
    private Produit produit;

    /**
     * Initialise le produit de test avec des valeurs par defaut avant chaque test.
     */
    @BeforeEach
    void setUp() {
        produit = new Produit("Ordinateur", "PC portable", 999.99, 10);
        produit.setId(1L);
    }

    /**
     * Teste le endpoint {@code GET /api/produits} : doit retourner 200 OK avec une liste JSON.
     */
    @Test
    @DisplayName("GET /api/produits -> 200 OK avec la liste des produits")
    void listerTous() throws Exception {
        when(produitService.listerTous()).thenReturn(List.of(produit));

        mockMvc.perform(get("/api/produits"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].nom").value("Ordinateur"))
            .andExpect(jsonPath("$[0].prix").value(999.99));
    }

    /**
     * Teste le endpoint {@code GET /api/produits/1} : produit existant -> 200 OK.
     */
    @Test
    @DisplayName("GET /api/produits/1 -> 200 OK")
    void trouverParId_existant() throws Exception {
        when(produitService.trouverParId(1L)).thenReturn(Optional.of(produit));

        mockMvc.perform(get("/api/produits/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    /**
     * Teste le endpoint {@code GET /api/produits/99} : produit inexistant -> 404 Not Found.
     */
    @Test
    @DisplayName("GET /api/produits/99 -> 404 Not Found")
    void trouverParId_inexistant() throws Exception {
        when(produitService.trouverParId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produits/99"))
            .andExpect(status().isNotFound());
    }

    /**
     * Teste le endpoint {@code POST /api/produits} avec un produit valide -> 201 Created.
     */
    @Test
    @DisplayName("POST /api/produits -> 201 Created")
    void creer_valide() throws Exception {
        when(produitService.creer(any(Produit.class))).thenReturn(produit);

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    /**
     * Teste le endpoint {@code POST /api/produits} avec un produit invalide -> 400 Bad Request.
     * La validation Jakarta (nom vide, prix negatif) doit rejeter la requete.
     */
    @Test
    @DisplayName("POST /api/produits avec nom vide -> 400 Bad Request")
    void creer_invalide() throws Exception {
        Produit invalide = new Produit("", "desc", -10, -1);

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalide)))
            .andExpect(status().isBadRequest());
    }

    /**
     * Teste le endpoint {@code PUT /api/produits/1} avec un produit valide -> 200 OK.
     */
    @Test
    @DisplayName("PUT /api/produits/1 -> 200 OK")
    void mettreAJour() throws Exception {
        when(produitService.mettreAJour(eq(1L), any(Produit.class)))
            .thenReturn(produit);

        mockMvc.perform(put("/api/produits/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    /**
     * Teste le endpoint {@code DELETE /api/produits/1} -> 204 No Content.
     */
    @Test
    @DisplayName("DELETE /api/produits/1 -> 204 No Content")
    void supprimer() throws Exception {
        doNothing().when(produitService).supprimer(1L);

        mockMvc.perform(delete("/api/produits/1"))
            .andExpect(status().isNoContent());
    }

    /**
     * Teste le endpoint {@code GET /api/produits/recherche?nom=Ordi} -> 200 OK
     * avec la liste des produits correspondants.
     */
    @Test
    @DisplayName("GET /api/produits/recherche?nom=Ordi -> 200 OK")
    void rechercherParNom() throws Exception {
        when(produitService.rechercherParNom("Ordi"))
            .thenReturn(List.of(produit));

        mockMvc.perform(get("/api/produits/recherche")
                .param("nom", "Ordi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Ordinateur"));
    }

    /**
     * Teste le endpoint {@code POST /api/produits} en cas de conflit de nom.
     * Le service leve une {@link IllegalArgumentException}, le controleur laisse
     * remonter l'exception en erreur 500.
     */
    @Test
    @DisplayName("POST creation avec conflit -> 500 (gere par le service)")
    void creer_conflit() throws Exception {
        when(produitService.creer(any(Produit.class)))
            .thenThrow(new IllegalArgumentException("Un produit avec ce nom existe deja"));

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isInternalServerError());
    }
}
