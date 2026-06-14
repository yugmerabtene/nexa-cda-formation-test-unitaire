package com.nexa.springintro.repository;

import com.nexa.springintro.model.Produit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Tests JPA avec {@code @DataJpaTest}</h1>
 *
 * <h2>Annotation {@code @DataJpaTest}</h2>
 * <p>
 * <b>Slice de test Spring</b> qui charge UNIQUEMENT la couche JPA :
 * </p>
 * <ul>
 *   <li>Les repositories (interfaces JPA)</li>
 *   <li>L'EntityManager et le datasource</li>
 *   <li>Par défaut, utilise une base H2 en mémoire (intégrée)</li>
 *   <li>Chaque test est rollback automatiquement à la fin</li>
 *   <li><b>NE charge PAS</b> : {@code @Service}, {@code @Controller}</li>
 * </ul>
 *
 * <h2>{@code @AutoConfigureTestDatabase(replace = ANY)}</h2>
 * <p>
 * Remplace le datasource configuré par un datasource embarqué (H2).
 * Garantit que les tests n'utilisent jamais la base de production.
 * </p>
 */
@DataJpaTest
@DisplayName("Tests du ProduitRepository")
class ProduitRepositoryTest {

    @Autowired
    private ProduitRepository repository;

    private Produit p1, p2, p3;

    @BeforeEach
    void setUp() {
        p1 = repository.save(new Produit("Ordinateur", "PC", 999.99, 10));
        p2 = repository.save(new Produit("Souris", "Sans fil", 29.99, 100));
        p3 = repository.save(new Produit("Clavier", "Mécanique", 89.99, 50));
    }

    @Test
    @DisplayName("findAll : retourne tous les produits")
    void findAll() {
        List<Produit> produits = repository.findAll();
        assertEquals(3, produits.size());
    }

    @Test
    @DisplayName("findById : retourne le produit par ID")
    void findById() {
        Optional<Produit> resultat = repository.findById(p1.getId());
        assertTrue(resultat.isPresent());
        assertEquals("Ordinateur", resultat.get().getNom());
    }

    @Test
    @DisplayName("findByNomContainingIgnoreCase : insensible à la casse")
    void rechercheInsensibleCasse() {
        List<Produit> r1 = repository.findByNomContainingIgnoreCase("ordi");
        assertEquals(1, r1.size());
        assertEquals("Ordinateur", r1.get(0).getNom());

        List<Produit> r2 = repository.findByNomContainingIgnoreCase("ORDI");
        assertEquals(1, r2.size());
    }

    @Test
    @DisplayName("findByPrixLessThanEqual : filtre par prix max")
    void filtrePrixMax() {
        List<Produit> abordables = repository.findByPrixLessThanEqual(99.99);
        assertEquals(2, abordables.size());
    }

    @Test
    @DisplayName("findByQuantiteGreaterThan : filtre par quantité min")
    void filtreQuantiteMin() {
        List<Produit> enStock = repository.findByQuantiteGreaterThan(49);
        assertEquals(2, enStock.size());
    }

    @Test
    @DisplayName("save : persiste un nouveau produit")
    void save() {
        Produit p = repository.save(new Produit("Écran", "27 pouces", 349.99, 15));
        assertNotNull(p.getId());
        assertEquals(4, repository.count());
    }

    @Test
    @DisplayName("deleteById : supprime un produit")
    void deleteById() {
        repository.deleteById(p1.getId());
        assertEquals(2, repository.count());
        assertFalse(repository.existsById(p1.getId()));
    }

    @Test
    @DisplayName("existsByNomIgnoreCase : détection de doublon")
    void existsByNom() {
        assertTrue(repository.existsByNomIgnoreCase("Ordinateur"));
        assertTrue(repository.existsByNomIgnoreCase("ordinateur"));
        assertFalse(repository.existsByNomIgnoreCase("Tablette"));
    }
}
