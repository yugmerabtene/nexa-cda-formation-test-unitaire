package com.nexa.springintro.repository;

import com.nexa.springintro.model.Produit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'integration du repository avec {@code @DataJpaTest}.
 * Utilise une base H2 en memoire et valide la couche de persistance JPA.
 */
@DataJpaTest
@DisplayName("Tests du ProduitRepository")
class ProduitRepositoryTest {

    /**
     * Repository injecte automatiquement par Spring Data JPA.
     */
    @Autowired
    private ProduitRepository repository;

    /**
     * Produit de test 1 : Ordinateur.
     */
    private Produit p1;

    /**
     * Produit de test 2 : Souris.
     */
    private Produit p2;

    /**
     * Produit de test 3 : Clavier.
     */
    private Produit p3;

    /**
     * Initialise la base de test avec trois produits avant chaque test.
     */
    @BeforeEach
    void setUp() {
        p1 = repository.save(new Produit("Ordinateur", "PC", 999.99, 10));
        p2 = repository.save(new Produit("Souris", "Sans fil", 29.99, 100));
        p3 = repository.save(new Produit("Clavier", "Mecanique", 89.99, 50));
    }

    /**
     * Teste la methode {@code findAll} : doit retourner les 3 produits inseres.
     */
    @Test
    @DisplayName("findAll : retourne tous les produits")
    void findAll() {
        List<Produit> produits = repository.findAll();
        assertEquals(3, produits.size());
    }

    /**
     * Teste la methode {@code findById} : doit retrouver le produit par son identifiant.
     */
    @Test
    @DisplayName("findById : retourne le produit par ID")
    void findById() {
        Optional<Produit> resultat = repository.findById(p1.getId());
        assertTrue(resultat.isPresent());
        assertEquals("Ordinateur", resultat.get().getNom());
    }

    /**
     * Teste la recherche insensible a la casse : "ordi" et "ORDI" doivent
     * retourner le meme produit "Ordinateur".
     */
    @Test
    @DisplayName("findByNomContainingIgnoreCase : insensible a la casse")
    void rechercheInsensibleCasse() {
        List<Produit> r1 = repository.findByNomContainingIgnoreCase("ordi");
        assertEquals(1, r1.size());
        assertEquals("Ordinateur", r1.get(0).getNom());

        List<Produit> r2 = repository.findByNomContainingIgnoreCase("ORDI");
        assertEquals(1, r2.size());
    }

    /**
     * Teste le filtre par prix maximal : seuls les produits <= 99.99 sont retournes
     * (Souris a 29.99 et Clavier a 89.99).
     */
    @Test
    @DisplayName("findByPrixLessThanEqual : filtre par prix max")
    void filtrePrixMax() {
        List<Produit> abordables = repository.findByPrixLessThanEqual(99.99);
        assertEquals(2, abordables.size());
    }

    /**
     * Teste le filtre par quantite minimale : seuls les produits avec quantite > 49
     * sont retournes (Souris a 100 et Clavier a 50).
     */
    @Test
    @DisplayName("findByQuantiteGreaterThan : filtre par quantite min")
    void filtreQuantiteMin() {
        List<Produit> enStock = repository.findByQuantiteGreaterThan(49);
        assertEquals(2, enStock.size());
    }

    /**
     * Teste la methode {@code save} : un nouveau produit persiste doit recevoir
     * un identifiant genere et le compte total doit augmenter.
     */
    @Test
    @DisplayName("save : persiste un nouveau produit")
    void save() {
        Produit p = repository.save(new Produit("Ecran", "27 pouces", 349.99, 15));
        assertNotNull(p.getId());
        assertEquals(4, repository.count());
    }

    /**
     * Teste la methode {@code deleteById} : apres suppression, le produit ne doit
     * plus exister et le compte total doit diminuer.
     */
    @Test
    @DisplayName("deleteById : supprime un produit")
    void deleteById() {
        repository.deleteById(p1.getId());
        assertEquals(2, repository.count());
        assertFalse(repository.existsById(p1.getId()));
    }

    /**
     * Teste la detection de doublon par nom : insensible a la casse.
     */
    @Test
    @DisplayName("existsByNomIgnoreCase : detection de doublon")
    void existsByNom() {
        assertTrue(repository.existsByNomIgnoreCase("Ordinateur"));
        assertTrue(repository.existsByNomIgnoreCase("ordinateur"));
        assertFalse(repository.existsByNomIgnoreCase("Tablette"));
    }
}
