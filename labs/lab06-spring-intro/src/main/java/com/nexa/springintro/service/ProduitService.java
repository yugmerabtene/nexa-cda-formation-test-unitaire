package com.nexa.springintro.service;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProduitService {

    private final ProduitRepository repository;

    public ProduitService(ProduitRepository repository) {
        this.repository = repository;
    }

    public List<Produit> listerTous() {
        return repository.findAll();
    }

    public Optional<Produit> trouverParId(Long id) {
        return repository.findById(id);
    }

    public Produit creer(Produit produit) {
        if (repository.existsByNomIgnoreCase(produit.getNom())) {
            throw new IllegalArgumentException("Un produit avec ce nom existe déjà");
        }
        return repository.save(produit);
    }

    public Produit mettreAJour(Long id, Produit produit) {
        Produit existant = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit introuvable : id=" + id));
        existant.setNom(produit.getNom());
        existant.setDescription(produit.getDescription());
        existant.setPrix(produit.getPrix());
        existant.setQuantite(produit.getQuantite());
        return repository.save(existant);
    }

    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Produit introuvable : id=" + id);
        }
        repository.deleteById(id);
    }

    public List<Produit> rechercherParNom(String nom) {
        return repository.findByNomContainingIgnoreCase(nom);
    }

    public List<Produit> filtrerParPrixMax(double prixMax) {
        return repository.findByPrixLessThanEqual(prixMax);
    }
}
