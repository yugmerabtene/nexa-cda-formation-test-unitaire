package com.nexa.springintro.controller;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST exposant les endpoints CRUD pour la gestion des produits.
 * Toutes les routes sont prefixees par {@code /api/produits}.
 */
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    /**
     * Service metier des produits, injecte par constructeur.
     */
    private final ProduitService service;

    /**
     * Constructeur avec injection du service metier.
     *
     * @param service le service de gestion des produits
     */
    public ProduitController(ProduitService service) {
        this.service = service;
    }

    /**
     * Liste tous les produits du catalogue.
     *
     * @return la liste de tous les produits
     */
    @GetMapping
    public List<Produit> listerTous() {
        return service.listerTous();
    }

    /**
     * Recupere un produit par son identifiant.
     *
     * @param id identifiant du produit
     * @return une réponse 200 avec le produit, ou 404 si introuvable
     */
    @GetMapping("/{id}")
    public ResponseEntity<Produit> trouverParId(@PathVariable Long id) {
        return service.trouverParId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cree un nouveau produit. Le corps de la requête est validé avec {@code @Valid}.
     *
     * @param produit le produit a creer (validé)
     * @return le produit créé avec le statut 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produit creer(@Valid @RequestBody Produit produit) {
        return service.creer(produit);
    }

    /**
     * Met a jour un produit existant identifie par son ID.
     *
     * @param id      identifiant du produit a modifier
     * @param produit les nouvelles valeurs du produit (validees)
     * @return le produit mis a jour
     */
    @PutMapping("/{id}")
    public Produit mettreAJour(@PathVariable Long id, @Valid @RequestBody Produit produit) {
        return service.mettreAJour(id, produit);
    }

    /**
     * Supprime un produit par son identifiant.
     *
     * @param id identifiant du produit a supprimer
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    /**
     * Recherche des produits par correspondance partielle sur le nom.
     *
     * @param nom fragment du nom a rechercher (paramètre de requête)
     * @return la liste des produits correspondants
     */
    @GetMapping("/recherche")
    public List<Produit> rechercher(@RequestParam String nom) {
        return service.rechercherParNom(nom);
    }
}
