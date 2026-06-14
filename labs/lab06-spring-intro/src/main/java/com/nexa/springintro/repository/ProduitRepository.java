package com.nexa.springintro.repository;

import com.nexa.springintro.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    List<Produit> findByNomContainingIgnoreCase(String nom);

    List<Produit> findByPrixLessThanEqual(double prixMax);

    List<Produit> findByQuantiteGreaterThan(int quantiteMin);

    boolean existsByNomIgnoreCase(String nom);
}
