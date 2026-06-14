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

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires du ProduitService")
class ProduitServiceTest {

    @Mock
    private ProduitRepository repository;

    @InjectMocks
    private ProduitService service;

    private Produit produit;

    @BeforeEach
    void setUp() {
        produit = new Produit("Clavier", "Clavier mécanique", 89.99, 25);
        produit.setId(1L);
    }

    @Test
    @DisplayName("creer : sauvegarde et retourne le produit")
    void creer() {
        when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(false);
        when(repository.save(any(Produit.class))).thenReturn(produit);

        Produit resultat = service.creer(produit);
        assertNotNull(resultat);
        assertEquals("Clavier", resultat.getNom());
    }

    @Test
    @DisplayName("creer : échoue si le nom existe déjà")
    void creer_nomExistant() {
        when(repository.existsByNomIgnoreCase("Clavier")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.creer(produit));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("trouverParId : retourne le produit")
    void trouverParId() {
        when(repository.findById(1L)).thenReturn(Optional.of(produit));
        Optional<Produit> resultat = service.trouverParId(1L);
        assertTrue(resultat.isPresent());
    }

    @Test
    @DisplayName("supprimer : supprime le produit existant")
    void supprimer() {
        when(repository.existsById(1L)).thenReturn(true);
        service.supprimer(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("supprimer : échoue si le produit n'existe pas")
    void supprimer_inexistant() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.supprimer(99L));
    }

    @Test
    @DisplayName("rechercherParNom : filtre par nom")
    void rechercherParNom() {
        when(repository.findByNomContainingIgnoreCase("clavier"))
            .thenReturn(List.of(produit));
        List<Produit> resultats = service.rechercherParNom("clavier");
        assertEquals(1, resultats.size());
    }
}
