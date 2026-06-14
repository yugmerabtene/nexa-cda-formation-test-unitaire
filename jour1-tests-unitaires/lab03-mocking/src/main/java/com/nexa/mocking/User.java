package com.nexa.mocking;

public class User {

    private Long id;
    private String nom;
    private String email;
    private boolean actif;

    public User() {}

    public User(Long id, String nom, String email, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.actif = actif;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public String toString() {
        return "User{id=" + id + ", nom='" + nom + "', email='" + email + "', actif=" + actif + "}";
    }
}
