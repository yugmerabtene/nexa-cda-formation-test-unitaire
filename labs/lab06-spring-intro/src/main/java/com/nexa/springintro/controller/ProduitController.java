package com.nexa.springintro.controller;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
