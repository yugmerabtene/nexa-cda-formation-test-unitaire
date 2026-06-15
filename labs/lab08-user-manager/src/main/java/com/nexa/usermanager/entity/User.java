package com.nexa.usermanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Entite JPA representant un utilisateur du système.
 *
 * <p>Cette classe est mappee sur la table {@code users} en base de donnees.
 * Chaque utilisateur possede un identifiant unique auto-genere, un nom,
 * un prenom, un email unique, un mot de passe hache, un role (USER ou ADMIN),
 * un statut actif/inactif, et des dates de creation et modification.</p>
 *
 * <p>Les contraintes de validation Jakarta Bean Validation sont appliquees
 * directement sur les champs pour garantir l'integrite des donnees.</p>
 */
@Entity
@Table(name = "users")
public class User {

    /** Identifiant unique auto-genere de l'utilisateur. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de famille de l'utilisateur (2 a 50 caracteres, obligatoire). */
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false, length = 50)
    private String nom;

    /** Prenom de l'utilisateur (2 a 50 caracteres, obligatoire). */
    @NotBlank
    @Size(min = 2, max = 50)
    @Column(nullable = false, length = 50)
    private String prenom;

    /** Adresse email unique de l'utilisateur (format email validé, obligatoire). */
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    /** Mot de passe hache de l'utilisateur (8 caracteres minimum, obligatoire). */
    @NotBlank
    @Size(min = 8)
    @Column(nullable = false)
    private String password;

    /** Role de l'utilisateur dans le système (USER par defaut). */
    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    /** Indique si le compte utilisateur est actif (true par defaut). */
    @Column(nullable = false)
    private boolean actif = true;

    /** Date et heure de creation du compte (initialisee a l'instant present). */
    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Date et heure de la dernière modification du compte. */
    private LocalDateTime dateModification;

    /**
     * Enumeration des roles disponibles dans le système.
     *
     * <ul>
     *   <li>{@code USER} : utilisateur standard avec accès en lecture seule.</li>
     *   <li>{@code ADMIN} : administrateur avec accès complèt (CRUD).</li>
     * </ul>
     */
    public enum Role { USER, ADMIN }

    /**
     * Constructeur par defaut requis par JPA.
     * <p>Initialise le role a {@code USER} et le statut actif a {@code true}.</p>
     */
    public User() {}

    /**
     * Constructeur paramètre pour creer un utilisateur avec ses informations principales.
     *
     * @param nom      le nom de famille de l'utilisateur
     * @param prenom   le prenom de l'utilisateur
     * @param email    l'adresse email unique
     * @param password le mot de passe en clair (sera hache avant persistance)
     * @param role     le role de l'utilisateur (USER ou ADMIN)
     */
    public User(String nom, String prenom, String email, String password, Role role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    /** @return l'identifiant unique de l'utilisateur */
    public Long getId() { return id; }
    /** @param id l'identifiant a definir */
    public void setId(Long id) { this.id = id; }
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
    /** @return le mot de passe hache */
    public String getPassword() { return password; }
    /** @param password le mot de passe a definir */
    public void setPassword(String password) { this.password = password; }
    /** @return le role de l'utilisateur */
    public Role getRole() { return role; }
    /** @param role le role a definir */
    public void setRole(Role role) { this.role = role; }
    /** @return {@code true} si le compte est actif, {@code false} sinon */
    public boolean isActif() { return actif; }
    /** @param actif le statut actif/inactif a definir */
    public void setActif(boolean actif) { this.actif = actif; }
    /** @return la date de creation du compte */
    public LocalDateTime getDateCreation() { return dateCreation; }
    /** @param dateCreation la date de creation a definir */
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    /** @return la date de dernière modification */
    public LocalDateTime getDateModification() { return dateModification; }
    /** @param dateModification la date de modification a definir */
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
