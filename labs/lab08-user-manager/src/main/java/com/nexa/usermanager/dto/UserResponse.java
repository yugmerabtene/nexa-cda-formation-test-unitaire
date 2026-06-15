package com.nexa.usermanager.dto;

import com.nexa.usermanager.entity.User;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) representant la reponse envoyee au client
 * lors de la consultation d'un utilisateur.
 *
 * <p>Ce DTO est conçu pour ne jamais exposer le mot de passe de l'utilisateur,
 * garantissant ainsi la securite des donnees sensibles.</p>
 *
 * <p>La methode statique {@link #from(User)} permet de convertir facilement
 * une entite JPA {@link User} en DTO de reponse.</p>
 */
public class UserResponse {

    /** Identifiant unique de l'utilisateur. */
    private Long id;
    /** Nom de famille de l'utilisateur. */
    private String nom;
    /** Prenom de l'utilisateur. */
    private String prenom;
    /** Adresse email de l'utilisateur. */
    private String email;
    /** Role de l'utilisateur (USER ou ADMIN). */
    private String role;
    /** Indique si le compte est actif. */
    private boolean actif;
    /** Date de creation du compte. */
    private LocalDateTime dateCreation;
    /** Date de derniere modification du compte. */
    private LocalDateTime dateModification;

    /**
     * Cree un {@code UserResponse} a partir d'une entite {@link User}.
     *
     * <p>Le mot de passe n'est <b>jamais</b> copie dans le DTO de reponse.</p>
     *
     * @param user l'entite JPA source
     * @return le DTO de reponse correspondant
     */
    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.nom = user.getNom();
        r.prenom = user.getPrenom();
        r.email = user.getEmail();
        r.role = user.getRole().name();
        r.actif = user.isActif();
        r.dateCreation = user.getDateCreation();
        r.dateModification = user.getDateModification();
        return r;
    }

    /** @return l'identifiant de l'utilisateur */
    public Long getId() { return id; }
    /** @param id l'identifiant a definir */
    public void setId(Long id) { this.id = id; }
    /** @return le nom de famille */
    public String getNom() { return nom; }
    /** @param nom le nom a definir */
    public void setNom(String nom) { this.nom = nom; }
    /** @return le prenom */
    public String getPrenom() { return prenom; }
    /** @param prenom le prenom a definir */
    public void setPrenom(String prenom) { this.prenom = prenom; }
    /** @return l'adresse email */
    public String getEmail() { return email; }
    /** @param email l'adresse email a definir */
    public void setEmail(String email) { this.email = email; }
    /** @return le role (USER ou ADMIN) */
    public String getRole() { return role; }
    /** @param role le role a definir */
    public void setRole(String role) { this.role = role; }
    /** @return {@code true} si le compte est actif */
    public boolean isActif() { return actif; }
    /** @param actif le statut a definir */
    public void setActif(boolean actif) { this.actif = actif; }
    /** @return la date de creation */
    public LocalDateTime getDateCreation() { return dateCreation; }
    /** @param dateCreation la date de creation a definir */
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    /** @return la date de derniere modification */
    public LocalDateTime getDateModification() { return dateModification; }
    /** @param dateModification la date de modification a definir */
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
