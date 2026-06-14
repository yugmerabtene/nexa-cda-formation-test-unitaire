package com.nexa.mocking;

/**
 * <h1>UserRepository — Interface simulant une couche d'accès aux données</h1>
 *
 * <p>
 * Dans une vraie application, cette interface serait implémentée par JPA, JDBC, etc.
 * Pour nos tests, on va la <b>mocker</b> avec Mockito : on simule son comportement
 * sans base de données réelle.
 * </p>
 *
 * <h2>Pourquoi une interface ?</h2>
 * <p>
 * Mockito peut mocker aussi bien des <b>classes concrètes</b> que des <b>interfaces</b>.
 * Mocker une interface est encore plus simple car il n'y a pas de constructeur.
 * </p>
 */
public interface UserRepository {

    User findById(Long id);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    void deleteById(Long id);
}
