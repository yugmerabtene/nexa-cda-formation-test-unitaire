package com.nexa.springintro.service;

import com.nexa.springintro.model.Produit;
import com.nexa.springintro.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service metier gerant les operations CRUD sur les produits.
 * Toutes les operations sont transactionnelles en lecture/ecriture.
 */
@Service
@Transactional
public class ProduitService {

    /**
     * Repository d'acces aux donnees des produits.
     */
    private final ProduitRepository repository;

    /**
     * Constructeur avec injection du repository.
     *
     * @param repository le repository de produits
     */
    public ProduitService(ProduitRepository repository) {
        this.repository = repository;
    }

    /**
     * Retourne la liste complete de tous les produits.
     *
     * @return liste de tous les produits en base
     */
    public List<Produit> listerTous() {
        return repository.findAll();
    }

    /**
     * Recherche un produit par son identifiant unique.
     *
     * @param id identifiant du produit
     * @return un {@link Optional} contenant le produit s'il existe, vide sinon
     */
    public Optional<Produit> trouverParId(Long id) {
        return repository.findById(id);
    }

    /**
     * Cree un nouveau produit apres verification de l'unicite du nom.
     *
     * @param produit le produit a creer
     * @return le produit persiste avec son identifiant genere
     * @throws IllegalArgumentException si un produit avec le meme nom existe deja
     */
    public Produit creer(Produit produit) {
        if (repository.existsByNomIgnoreCase(produit.getNom())) {
            throw new IllegalArgumentException("Un produit avec ce nom existe deja");
        }
        return repository.save(produit);
    }

    /**
     * Met a jour un produit existant. Le produit doit prealablement exister en base.
     *
     * @param id      identifiant du produit a modifier
     * @param produit les nouvelles valeurs du produit
     * @return le produit mis a jour
     * @throws IllegalArgumentException si le produit est introuvable
     */
    public Produit mettreAJour(Long id, Produit produit) {
        Produit existant = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Produit introuvable : id=" + id));
        existant.setNom(produit.getNom());
        existant.setDescription(produit.getDescription());
        existant.setPrix(produit.getPrix());
        existant.setQuantite(produit.getQuantite());
        return repository.save(existant);
    }

    /**
     * Supprime un produit par son identifiant.
     *
     * @param id identifiant du produit a supprimer
     * @throws IllegalArgumentException si le produit n'existe pas
     */
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Produit introuvable : id=" + id);
        }
        repository.deleteById(id);
    }

    /**
     * Recherche des produits par correspondance partielle sur le nom, insensible a la casse.
     *
     * @param nom fragment du nom a rechercher
     * @return liste des produits dont le nom contient le fragment
     */
    public List<Produit> rechercherParNom(String nom) {
        return repository.findByNomContainingIgnoreCase(nom);
    }

    /**
     * Filtre les produits ayant un prix inferieur ou egal au seuil donne.
     *
     * @param prixMax prix maximum (inclus)
     * @return liste des produits respectant le budget maximum
     */
    public List<Produit> filtrerParPrixMax(double prixMax) {
        return repository.findByPrixLessThanEqual(prixMax);
    }
}
