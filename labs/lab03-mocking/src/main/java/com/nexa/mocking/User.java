package com.nexa.mocking;

/**
 * Entite representant un utilisateur.
 *
 * Cette classe est un POJO (Plain Old Java Object) avec :
 * - Des champs prives
 * - Des getters/setters publics
 * - Un constructeur par defaut (necessaire pour certaines librairies)
 * - Un constructeur parametre
 * - Une methode toString() pour le debugging
 *
 * Les setters permettent a Mockito et aux tests de manipuler l'objet.
 */
public class User {

    /** Identifiant unique (peut etre null avant la persistance) */
    private Long id;

    /** Nom de l'utilisateur */
    private String nom;

    /** Adresse email (unique en base) */
    private String email;

    /** Indique si le compte est actif ou desactive */
    private boolean actif;

    /**
     * Constructeur par defaut.
     * Necessaire pour certaines librairies (Jackson, Hibernate, etc.)
     * qui creent des instances par reflexion.
     */
    public User() {}

    /**
     * Constructeur avec tous les champs.
     *
     * @param id l'identifiant (peut etre null pour une creation)
     * @param nom le nom de l'utilisateur
     * @param email l'adresse email
     * @param actif l'etat du compte (true = actif)
     */
    public User(Long id, String nom, String email, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.actif = actif;
    }

    // --- Getters et Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    /**
     * Getter booleen avec la convention JavaBeans : prefixe 'is' au lieu de 'get'.
     *
     * @return true si le compte est actif
     */
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    /**
     * Representation texte de l'utilisateur pour le debugging.
     *
     * @return une chaine descriptive (ex: "User{id=1, nom='Alice', ...}")
     */
    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', email='" + email + "', actif=" + actif + "}";
    }
}
