package com.nexa.usermanager.service;

import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.exception.ResourceNotFoundException;
import com.nexa.usermanager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User creer(User user) {
        if (repo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    public List<User> listerTous() {
        return repo.findAll();
    }

    public Page<User> listerPagine(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public User trouverParId(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : id=" + id));
    }

    public Optional<User> trouverParEmail(String email) {
        return repo.findByEmail(email);
    }

    public User mettreAJour(Long id, User update) {
        User existant = trouverParId(id);
        existant.setNom(update.getNom());
        existant.setPrenom(update.getPrenom());
        existant.setEmail(update.getEmail());
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existant.setPassword(encoder.encode(update.getPassword()));
        }
        existant.setRole(update.getRole());
        existant.setActif(update.isActif());
        existant.setDateModification(LocalDateTime.now());
        return repo.save(existant);
    }

    public void supprimer(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable : id=" + id);
        }
        repo.deleteById(id);
    }

    public void desactiver(Long id) {
        User u = trouverParId(id);
        u.setActif(false);
        repo.save(u);
    }

    public List<User> rechercherParNom(String nom) {
        return repo.findByNomContainingIgnoreCase(nom);
    }

    public List<User> listerActifs() {
        return repo.findByActif(true);
    }

    public long compterParRole(User.Role role) {
        return repo.countByRole(role);
    }
}
