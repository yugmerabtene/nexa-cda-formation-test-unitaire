package com.nexa.secu.entity;

import jakarta.persistence.*;

/**
 * Entite JPA representant un utilisateur de l'application.
 * <p>
 * Mappee sur la table {@code utilisateurs}, cette entite stocke les informations
 * d'authentification et le role (ADMIN ou USER) de chaque compte. Le mot de passe
 * est chiffre avec BCrypt avant d'etre enregistre.
 */
@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

    /** Identifiant unique genere automatiquement par la base de donnees. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom d'utilisateur unique, obligatoire, utilise pour l'authentification. */
    @Column(unique = true, nullable = false)
    private String username;

    /** Mot de passe chiffre (BCrypt), obligatoire. */
    @Column(nullable = false)
    private String password;

    /** Role de l'utilisateur (ex: "ADMIN", "USER"). Obligatoire. */
    @Column(nullable = false)
    private String role;

    /** Indique si le compte est actif. Valeur par defaut : {@code true}. */
    private boolean actif = true;

    /** Constructeur par defaut requis par JPA. */
    public Utilisateur() {}

    /**
     * Constructeur parametre utilise lors de la creation d'un nouvel utilisateur.
     *
     * @param username le nom d'utilisateur (unique)
     * @param password le mot de passe deja chiffre avec BCrypt
     * @param role     le role de l'utilisateur (ex: "ADMIN", "USER")
     */
    public Utilisateur(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /** @return l'identifiant unique de l'utilisateur */
    public Long getId() { return id; }

    /** @param id l'identifiant a definir */
    public void setId(Long id) { this.id = id; }

    /** @return le nom d'utilisateur */
    public String getUsername() { return username; }

    /** @param username le nom d'utilisateur a definir */
    public void setUsername(String username) { this.username = username; }

    /** @return le mot de passe chiffre */
    public String getPassword() { return password; }

    /** @param password le mot de passe chiffre a definir */
    public void setPassword(String password) { this.password = password; }

    /** @return le role de l'utilisateur (ex: "ADMIN", "USER") */
    public String getRole() { return role; }

    /** @param role le role a definir */
    public void setRole(String role) { this.role = role; }

    /** @return {@code true} si le compte est actif, {@code false} sinon */
    public boolean isActif() { return actif; }

    /** @param actif l'etat d'activation du compte */
    public void setActif(boolean actif) { this.actif = actif; }
}
