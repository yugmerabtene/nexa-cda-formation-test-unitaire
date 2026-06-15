package com.nexa.mocking;

/**
 * Interface du repository d'utilisateurs.
 *
 * Cette interface sera mockee dans les tests unitaires de UserService
 * pour isoler la couche metier de la couche d'accès aux donnees.
 *
 * En production, cette interface serait implementee par une classe
 * utilisant JPA, JDBC, ou tout autre mecanisme de persistance.
 * En test, Mockito créé un mock qui simule les appels sans base de donnees.
 *
 * @see UserService
 * @see UserServiceTest
 */
public interface UserRepository {

    /**
     * Recherche un utilisateur par son identifiant.
     *
     * @param id l'identifiant de l'utilisateur
     * @return l'utilisateur trouve, ou null s'il n'existe pas
     */
    User findById(Long id);

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email l'email a rechercher
     * @return l'utilisateur trouve, ou null
     */
    User findByEmail(String email);

    /**
     * Verifie si un email est déjà utilisé par un utilisateur existant.
     *
     * @param email l'email a verifier
     * @return true si l'email existe déjà en base
     */
    boolean existsByEmail(String email);

    /**
     * Sauvegarde un utilisateur (creation ou mise a jour).
     *
     * En base de donnees réelle, cette méthode attribuerait un ID
     * automatiquement. Dans les tests, on simule ce comportement avec
     * thenAnswer() ou en retournant l'objet tel quel.
     *
     * @param user l'utilisateur a sauvegarder
     * @return l'utilisateur sauvegarde (avec ID si creation)
     */
    User save(User user);

    /**
     * Supprime un utilisateur par son identifiant.
     *
     * @param id l'identifiant de l'utilisateur a supprimer
     */
    void deleteById(Long id);
}
