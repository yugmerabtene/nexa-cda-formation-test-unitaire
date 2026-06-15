package com.nexa.secu.repository;

import com.nexa.secu.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entite {@link Utilisateur}.
 * <p>
 * Fournit les methodes d'acces aux donnees pour la table {@code utilisateurs},
 * incluant la recherche par nom d'utilisateur et la verification d'existence.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /**
     * Recherche un utilisateur par son nom d'utilisateur.
     *
     * @param username le nom d'utilisateur a rechercher
     * @return un {@link Optional} contenant l'utilisateur s'il existe, vide sinon
     */
    Optional<Utilisateur> findByUsername(String username);

    /**
     * Verifie si un utilisateur avec ce nom d'utilisateur existe deja en base.
     *
     * @param username le nom d'utilisateur a verifier
     * @return {@code true} si le nom d'utilisateur existe, {@code false} sinon
     */
    boolean existsByUsername(String username);
}
