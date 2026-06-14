package com.nexa.usermanager.dto;

import com.nexa.usermanager.entity.User;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
