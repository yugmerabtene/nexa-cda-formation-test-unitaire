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
 * <h1>Tests MVC avec {@code @WebMvcTest}</h1>
 *
 * <h2>Annotation {@code @WebMvcTest(ProduitController.class)}</h2>
 * <p>
 * <b>Slice de test Spring</b> qui charge UNIQUEMENT la couche MVC :
 * </p>
 * <ul>
 *   <li>Le contrôleur spécifié (et ses dépendances)</li>
 *   <li>Les convertisseurs HTTP, la validation, la gestion d'erreurs</li>
 *   <li>{@code MockMvc} auto-configuré</li>
 *   <li><b>NE charge PAS</b> : {@code @Service}, {@code @Repository}, la base de données</li>
 * </ul>
 * <p>
 * Cela rend les tests <b>très rapides</b> (pas de contexte Spring complet).
 * </p>
 *
 * <h2>Annotation {@code @MockBean}</h2>
 * <p>
 * Remplace un bean Spring dans le contexte par un mock Mockito.
 * Ici, {@code ProduitService} est mocké car {@code @WebMvcTest}
 * ne charge pas les services. Cela isole le contrôleur de sa
 * dépendance métier.
 * </p>
 *
 * <h2>Annotation {@code @AutoConfigureMockMvc}</h2>
 * <p>
 * Incluse automatiquement par {@code @WebMvcTest}. Configure
 * {@code MockMvc} pour simuler des requêtes HTTP sans démarrer
 * de serveur.
 * </p>
 *
 * <h2>{@code MockMvc}</h2>
 * <p>
 * Classe principale pour tester les contrôleurs. Elle permet de :
 * </p>
 * <ul>
 *   <li>Construire des requêtes HTTP : {@code get()}, {@code post()}, etc.</li>
 *   <li>Définir le contenu de la requête : {@code content(json)}</li>
 *   <li>Vérifier la réponse : statut HTTP, corps JSON, headers</li>
 * </ul>
 */
@WebMvcTest(ProduitController.class)
@DisplayName("Tests MVC du ProduitController")
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProduitService produitService;

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit("Ordinateur", "PC portable", 999.99, 10);
        produit.setId(1L);
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * Tests GET (lecture)
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("GET /api/produits → 200 OK avec la liste des produits")
    void listerTous() throws Exception {
        when(produitService.listerTous()).thenReturn(List.of(produit));

        mockMvc.perform(get("/api/produits"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].nom").value("Ordinateur"))
            .andExpect(jsonPath("$[0].prix").value(999.99));
    }

    @Test
    @DisplayName("GET /api/produits/1 → 200 OK")
    void trouverParId_existant() throws Exception {
        when(produitService.trouverParId(1L)).thenReturn(Optional.of(produit));

        mockMvc.perform(get("/api/produits/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    @Test
    @DisplayName("GET /api/produits/99 → 404 Not Found")
    void trouverParId_inexistant() throws Exception {
        when(produitService.trouverParId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produits/99"))
            .andExpect(status().isNotFound());
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * Tests POST (création)
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("POST /api/produits → 201 Created")
    void creer_valide() throws Exception {
        when(produitService.creer(any(Produit.class))).thenReturn(produit);

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    @Test
    @DisplayName("POST /api/produits avec nom vide → 400 Bad Request")
    void creer_invalide() throws Exception {
        Produit invalide = new Produit("", "desc", -10, -1);

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalide)))
            .andExpect(status().isBadRequest());
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * Tests PUT (mise à jour)
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("PUT /api/produits/1 → 200 OK")
    void mettreAJour() throws Exception {
        when(produitService.mettreAJour(eq(1L), any(Produit.class)))
            .thenReturn(produit);

        mockMvc.perform(put("/api/produits/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nom").value("Ordinateur"));
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * Tests DELETE
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("DELETE /api/produits/1 → 204 No Content")
    void supprimer() throws Exception {
        doNothing().when(produitService).supprimer(1L);

        mockMvc.perform(delete("/api/produits/1"))
            .andExpect(status().isNoContent());
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * Tests de recherche
     * ────────────────────────────────────────────────────────────────────
     */

    @Test
    @DisplayName("GET /api/produits/recherche?nom=Ordi → 200 OK")
    void rechercherParNom() throws Exception {
        when(produitService.rechercherParNom("Ordi"))
            .thenReturn(List.of(produit));

        mockMvc.perform(get("/api/produits/recherche")
                .param("nom", "Ordi"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nom").value("Ordinateur"));
    }

    @Test
    @DisplayName("POST création avec conflit → 500 (géré par le service)")
    void creer_conflit() throws Exception {
        when(produitService.creer(any(Produit.class)))
            .thenThrow(new IllegalArgumentException("Un produit avec ce nom existe déjà"));

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(produit)))
            .andExpect(status().isInternalServerError());
    }
}
