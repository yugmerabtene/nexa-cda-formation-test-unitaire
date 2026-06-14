package com.nexa.springintro.controller;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h1>ProduitController — API REST</h1>
 *
 * <h2>{@code @RestController}</h2>
 * <p>Combine {@code @Controller} + {@code @ResponseBody} :
 * chaque méthode retourne directement le corps de la réponse HTTP
 * (JSON par défaut avec Jackson).</p>
 *
 * <h2>{@code @RequestMapping}</h2>
 * <p>Préfixe commun à toutes les routes du contrôleur.</p>
 *
 * <h2>{@code @GetMapping, @PostMapping, @PutMapping, @DeleteMapping}</h2>
 * <p>Raccourcis pour {@code @RequestMapping(method = ...)} qui lient
 * une méthode HTTP à une URL.</p>
 *
 * <h2>{@code @PathVariable}</h2>
 * <p>Extrait une variable de l'URL. Ex: /api/produits/42 → id = 42.</p>
 *
 * <h2>{@code @RequestBody}</h2>
 * <p>Désérialise le corps JSON de la requête en objet Java.</p>
 *
 * <h2>{@code @Valid}</h2>
 * <p>Déclenche la validation Bean Validation sur l'objet.
 * Si la validation échoue, Spring retourne automatiquement 400 Bad Request.</p>
 *
 * <h2>{@code @ResponseStatus}</h2>
 * <p>Définit le code de statut HTTP de la réponse.</p>
 */
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    private final ProduitService service;

    public ProduitController(ProduitService service) {
        this.service = service;
    }

    @GetMapping
    public List<Produit> listerTous() {
        return service.listerTous();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produit> trouverParId(@PathVariable Long id) {
        return service.trouverParId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produit creer(@Valid @RequestBody Produit produit) {
        return service.creer(produit);
    }

    @PutMapping("/{id}")
    public Produit mettreAJour(@PathVariable Long id, @Valid @RequestBody Produit produit) {
        return service.mettreAJour(id, produit);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @GetMapping("/recherche")
    public List<Produit> rechercher(@RequestParam String nom) {
        return service.rechercherParNom(nom);
    }
}
