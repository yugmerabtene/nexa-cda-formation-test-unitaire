package com.nexa.secu.service;

import com.nexa.secu.entity.Utilisateur;
import com.nexa.secu.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Utilisateur creer(String username, String password, String role) {
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Nom d'utilisateur déjà pris");
        }
        Utilisateur u = new Utilisateur(username, passwordEncoder.encode(password), role);
        return repository.save(u);
    }
}
