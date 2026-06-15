package com.nexa.springintro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Entite JPA representant un produit dans le catalogue.
 * Mappe sur la table <em>produits</em> et integre les contraintes de validation.
 */
@Entity
@Table(name = "produits")
public class Produit {

    /**
     * Identifiant unique du produit, genere automatiquement par la base de donnees.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nom du produit. Obligatoire, entre 2 et 100 caracteres.
     */
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nom;

    /**
     * Description optionnelle du produit, limitee a 500 caracteres.
     */
    @Size(max = 500, message = "La description ne peut pas depasser 500 caracteres")
    @Column(length = 500)
    private String description;

    /**
     * Prix du produit. Doit etre strictement positif.
     */
    @Positive(message = "Le prix doit etre strictement positif")
    @Column(nullable = false)
    private double prix;

    /**
     * Quantite en stock. Comprise entre 0 et 100 000.
     */
    @Min(value = 0, message = "La quantite ne peut pas etre negative")
    @Max(value = 100000, message = "La quantite maximale est 100 000")
    @Column(nullable = false)
    private int quantite;

    /**
     * Constructeur par defaut, requis par JPA.
     */
    public Produit() {}

    /**
     * Constructeur paramètre pratique pour les tests et les insertions.
     *
     * @param nom         nom du produit
     * @param description description du produit
     * @param prix        prix du produit
     * @param quantite    quantite en stock
     */
    public Produit(String nom, String description, double prix, int quantite) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.quantite = quantite;
    }

    /**
     * @return l'identifiant unique du produit
     */
    public Long getId() { return id; }

    /**
     * @param id identifiant du produit
     */
    public void setId(Long id) { this.id = id; }

    /**
     * @return le nom du produit
     */
    public String getNom() { return nom; }

    /**
     * @param nom nom du produit
     */
    public void setNom(String nom) { this.nom = nom; }

    /**
     * @return la description du produit
     */
    public String getDescription() { return description; }

    /**
     * @param description description du produit
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * @return le prix du produit
     */
    public double getPrix() { return prix; }

    /**
     * @param prix prix du produit
     */
    public void setPrix(double prix) { this.prix = prix; }

    /**
     * @return la quantite en stock
     */
    public int getQuantite() { return quantite; }

    /**
     * @param quantite quantite en stock
     */
    public void setQuantite(int quantite) { this.quantite = quantite; }
}
