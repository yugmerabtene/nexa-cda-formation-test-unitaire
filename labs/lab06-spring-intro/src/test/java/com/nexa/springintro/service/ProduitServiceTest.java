package com.nexa.springintro.service;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.repository.ProduitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service metier {@link ProduitService} avec Mockito.
 * Le repository est simule avec {@code @Mock} et injecte via {@code @InjectMocks}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du ProduitService")
class ProduitServiceTest {

    /**
     * Simulation du repository de produits.
     */
    @Mock
    private ProduitRepository repository;

    /**
     * Service sous test, avec le mock repository injecte automatiquement.
     */
    @InjectMocks
    private ProduitService service;

    /**
     * Produit de test reinitialise avant chaque test.
     */
    private Produit produit;

    /**
     * Initialise le produit de test avec un Clavier mecanique a 89.99.
     */
    @BeforeEach
    void setUp() {
        produit = new Produit("Clavier", "Clavier mecanique", 89.99, 25);
        produit.setId(1L);
    }

    /**
     * Teste la methode {@code creer} : sauvegarde et retourne le produit
     * quand le nom n'existe pas encore en base.
     */
    @Test
    @DisplayName("creer : sauvegarde et retourne le produit")
    void creer() {
        when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(false);
        when(repository.save(any(Produit.class))).thenReturn(produit);

        Produit resultat = service.creer(produit);
        assertNotNull(resultat);
        assertEquals("Clavier", resultat.getNom());
    }

    /**
     * Teste la methode {@code creer} en cas de conflit : leve une exception
     * si un produit avec le meme nom existe deja, sans appeler {@code save}.
     */
    @Test
    @DisplayName("creer : echoue si le nom existe deja")
    void creer_nomExistant() {
        when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.creer(produit));
        verify(repository, never()).save(any());
    }

    /**
     * Teste la methode {@code trouverParId} : retourne un {@link Optional}
     * contenant le produit quand il existe.
     */
    @Test
    @DisplayName("trouverParId : retourne le produit")
    void trouverParId() {
        when(repository.findById(1L)).thenReturn(Optional.of(produit));
        Optional<Produit> resultat = service.trouverParId(1L);
        assertTrue(resultat.isPresent());
    }

    /**
     * Teste la methode {@code supprimer} : supprime le produit quand il existe.
     * Verifie que {@code deleteById} est bien appele.
     */
    @Test
    @DisplayName("supprimer : supprime le produit existant")
    void supprimer() {
        when(repository.existsById(1L)).thenReturn(true);
        service.supprimer(1L);
        verify(repository).deleteById(1L);
    }

    /**
     * Teste la methode {@code supprimer} quand le produit n'existe pas :
     * doit lever une {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("supprimer : echoue si le produit n'existe pas")
    void supprimer_inexistant() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.supprimer(99L));
    }

    /**
     * Teste la methode {@code rechercherParNom} : retourne les produits
     * correspondant au fragment de nom fourni.
     */
    @Test
    @DisplayName("rechercherParNom : filtre par nom")
    void rechercherParNom() {
        when(repository.findByNomContainingIgnoreCase("clavier"))
            .thenReturn(List.of(produit));
        List<Produit> resultats = service.rechercherParNom("clavier");
        assertEquals(1, resultats.size());
    }
}
