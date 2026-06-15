package com.nexa.usermanager.dto;

import jakarta.validation.constraints.*;

/**
 * DTO (Data Transfer Object) representant la requête de creation ou mise a jour d'un utilisateur.
 *
 * <p>Cette classe est utilisé́e pour deserialiser le corps JSON des requetes HTTP
 * entrantes sur les endpoints de creation ({@code POST /api/users}) et de mise a jour
 * ({@code PUT /api/users/{id}}).</p>
 *
 * <p>Les annotations Jakarta Bean Validation garantissent que les donnees fournies
 * respectent les contraintes metier avant d'atteindre la couche service.</p>
 */
public class UserRequest {

    /** Nom de famille (obligatoire, entre 2 et 50 caracteres). */
    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caracteres")
    private String nom;

    /** Prenom (obligatoire, entre 2 et 50 caracteres). */
    @NotBlank(message = "Le prenom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prenom doit contenir entre 2 et 50 caracteres")
    private String prenom;

    /** Adresse email (obligatoire, format email validé). */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    /** Mot de passe (obligatoire, au moins 8 caracteres). */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String password;

    /** Role de l'utilisateur sous forme de chaine (obligatoire, valeur "USER" ou "ADMIN"). */
    @NotNull(message = "Le role est obligatoire")
    private String role;

    /** @return le nom de famille */
    public String getNom() { return nom; }
    /** @param nom le nom de famille a definir */
    public void setNom(String nom) { this.nom = nom; }
    /** @return le prenom */
    public String getPrenom() { return prenom; }
    /** @param prenom le prenom a definir */
    public void setPrenom(String prenom) { this.prenom = prenom; }
    /** @return l'adresse email */
    public String getEmail() { return email; }
    /** @param email l'adresse email a definir */
    public void setEmail(String email) { this.email = email; }
    /** @return le mot de passe en clair */
    public String getPassword() { return password; }
    /** @param password le mot de passe a definir */
    public void setPassword(String password) { this.password = password; }
    /** @return le role sous forme de chaine de caracteres */
    public String getRole() { return role; }
    /** @param role le role a definir */
    public void setRole(String role) { this.role = role; }
}
