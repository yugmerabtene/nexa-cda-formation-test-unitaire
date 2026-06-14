package com.nexa.springintro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * <h1>Produit — Entité JPA avec validation Bean Validation</h1>
 *
 * <h2>Annotations JPA</h2>
 * <ul>
 *   <li>{@code @Entity} — Cette classe est une entité JPA, mappée à une table</li>
 *   <li>{@code @Table(name = "produits")} — Nom explicite de la table</li>
 *   <li>{@code @Id} — Clé primaire</li>
 *   <li>{@code @GeneratedValue(strategy = GenerationType.IDENTITY)} —
 *       L'ID est auto-généré par la base (AUTO_INCREMENT)</li>
 *   <li>{@code @Column(nullable = false)} — Colonne NOT NULL dans la base</li>
 * </ul>
 *
 * <h2>Annotations Bean Validation (Jakarta)</h2>
 * <ul>
 *   <li>{@code @NotBlank} — La chaîne ne doit pas être null ni vide ni composée uniquement d'espaces</li>
 *   <li>{@code @Size(min, max)} — Longueur minimale/maximale de la chaîne</li>
 *   <li>{@code @Positive} — Le nombre doit être strictement positif (> 0)</li>
 *   <li>{@code @Min(value)} — Valeur minimale (inclusive)</li>
 *   <li>{@code @Max(value)} — Valeur maximale (inclusive)</li>
 * </ul>
 */
@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String nom;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String description;

    @Positive(message = "Le prix doit être strictement positif")
    @Column(nullable = false)
    private double prix;

    @Min(value = 0, message = "La quantité ne peut pas être négative")
    @Max(value = 100000, message = "La quantité maximale est 100 000")
    @Column(nullable = false)
    private int quantite;

    public Produit() {}

    public Produit(String nom, String description, double prix, int quantite) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
}
