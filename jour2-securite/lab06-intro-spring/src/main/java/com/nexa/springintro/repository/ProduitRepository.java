package com.nexa.springintro.repository;

import com.nexa.springintro.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h1>ProduitRepository — Interface d'accès aux données</h1>
 *
 * <h2>{@code @Repository}</h2>
 * <p>
 * Stéréotype Spring pour la couche d'accès aux données.
 * Spring crée automatiquement un bean, traduit les exceptions JPA en
 * {@code DataAccessException} et active le scan de composants.
 * </p>
 *
 * <h2>{@code JpaRepository&lt;T, ID&gt;}</h2>
 * <p>
 * Fournit des méthodes CRUD prêtes à l'emploi :
 * </p>
 * <ul>
 *   <li>{@code findAll()} — tous les enregistrements</li>
 *   <li>{@code findById(ID)} — par clé primaire</li>
 *   <li>{@code save(T)} — insert ou update</li>
 *   <li>{@code deleteById(ID)} — suppression</li>
 *   <li>{@code count()} — nombre d'enregistrements</li>
 *   <li>{@code existsById(ID)} — vérification d'existence</li>
 * </ul>
 *
 * <h2>Méthodes de requête dérivées (Query Methods)</h2>
 * <p>
 * Spring Data génère AUTOMATIQUEMENT la requête JPQL à partir du nom de la méthode.
 * Syntaxe : {@code findBy + NomAttribut + Opérateur (optionnel)}.
 * </p>
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByNomContainingIgnoreCase(String nom);

    List<Produit> findByPrixLessThanEqual(double prixMax);

    List<Produit> findByQuantiteGreaterThan(int quantiteMin);

    boolean existsByNomIgnoreCase(String nom);
}
