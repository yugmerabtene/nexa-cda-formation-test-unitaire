package com.nexa.secu.service;

import com.nexa.secu.entity.Utilisateur;
import com.nexa.secu.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service metier pour la gestion des utilisateurs.
 * <p>
 * Encapsule la logique de creation de comptes utilisateur avec verification
 * d'unicite du nom d'utilisateur et chiffrement du mot de passe via BCrypt.
 */
@Service
public class UtilisateurService {

    /** Repository pour l'accès aux donnees des utilisateurs. */
    private final UtilisateurRepository repository;

    /** Encodeur de mots de passe pour le chiffrement BCrypt. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructeur avec injection de dépendances.
     *
     * @param repository      le repository d'accès aux utilisateurs
     * @param passwordEncoder l'encodeur de mots de passe BCrypt
     */
    public UtilisateurService(UtilisateurRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cree un nouvel utilisateur après verification de l'unicite du username
     * et chiffrement du mot de passe.
     *
     * @param username le nom d'utilisateur souhaite (doit etre unique)
     * @param password le mot de passe en clair (sera chiffre avant stockage)
     * @param role     le role de l'utilisateur (ex: "ADMIN", "USER")
     * @return l'utilisateur créé et persiste en base de donnees
     * @throws IllegalArgumentException si le nom d'utilisateur est déjà pris
     */
    public Utilisateur creer(String username, String password, String role) {
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Nom d'utilisateur déjà pris");
        }
        Utilisateur u = new Utilisateur(username, passwordEncoder.encode(password), role);
        return repository.save(u);
    }
}
