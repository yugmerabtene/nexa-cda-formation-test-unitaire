package com.nexa.usermanager.repository;

import com.nexa.usermanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface repository pour l'entité {@link User}.
 *
 * <p>Ce repository etend {@link JpaRepository} et fournit des méthodes
 * de requête derivees pour les operations courantes de recherche
 * et de filtrage des utilisateurs.</p>
 *
 * <p>Les méthodes sont automatiquement implementees par Spring Data JPA
 * en fonction de leur signature (convention de nommage).</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email l'adresse email a rechercher
     * @return un {@link Optional} contenant l'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifie si un utilisateur existe avec l'adresse email donnee.
     *
     * @param email l'adresse email a verifier
     * @return {@code true} si un utilisateur possede cet email
     */
    boolean existsByEmail(String email);

    /**
     * Recherche des utilisateurs dont le nom contient la chaine donnee,
     * en ignorant la casse.
     *
     * @param nom la chaine de recherche partielle
     * @return la liste des utilisateurs correspondants
     */
    List<User> findByNomContainingIgnoreCase(String nom);

    /**
     * Filtre les utilisateurs par statut actif/inactif.
     *
     * @param actif {@code true} pour les utilisateurs actifs, {@code false} pour les inactifs
     * @return la liste des utilisateurs correspondant au filtre
     */
    List<User> findByActif(boolean actif);

    /**
     * Recherche paginee des utilisateurs par role.
     *
     * @param role     le role a filtrer (USER ou ADMIN)
     * @param pageable les informations de pagination (page, taille, tri)
     * @return une page d'utilisateurs correspondant au filtre
     */
    Page<User> findByRole(User.Role role, Pageable pageable);

    /**
     * Compte le nombre d'utilisateurs possedant un role donne.
     *
     * @param role le role pour lequel compter les utilisateurs
     * @return le nombre d'utilisateurs avec ce role
     */
    long countByRole(User.Role role);
}
