package com.nexa.springintro.repository;

import com.nexa.springintro.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour l'entite {@link Produit}.
 * Fournit des methodes de requete derivees pour interroger la table produits.
 */
@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    /**
     * Recherche les produits dont le nom contient la chaine fournie, sans distinction de casse.
     *
     * @param nom fragment du nom a rechercher
     * @return la liste des produits correspondants
     */
    List<Produit> findByNomContainingIgnoreCase(String nom);

    /**
     * Filtre les produits dont le prix est inferieur ou egal au prix maximum donne.
     *
     * @param prixMax prix maximum (inclus)
     * @return la liste des produits dans le budget indique
     */
    List<Produit> findByPrixLessThanEqual(double prixMax);

    /**
     * Filtre les produits dont la quantite est strictement superieure au seuil donne.
     *
     * @param quantiteMin quantite minimale (exclue)
     * @return la liste des produits ayant au moins quantiteMin+1 unites
     */
    List<Produit> findByQuantiteGreaterThan(int quantiteMin);

    /**
     * Verifie si un produit portant exactement ce nom existe deja (insensible a la casse).
     *
     * @param nom nom du produit a verifier
     * @return true si un produit avec ce nom existe, false sinon
     */
    boolean existsByNomIgnoreCase(String nom);
}
