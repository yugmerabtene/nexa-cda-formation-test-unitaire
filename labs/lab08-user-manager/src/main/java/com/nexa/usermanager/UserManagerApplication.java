package com.nexa.usermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree principal de l'application de gestion d'utilisateurs.
 *
 * <p>Cette classe lance l'application Spring Boot avec auto-configuration,
 * scan des composants et configuration automatique.</p>
 *
 * <p>L'application expose une API REST securisee par JWT pour la gestion
 * complète des utilisateurs (CRUD), avec controle d'accès base sur les
 * roles (ADMIN et USER).</p>
 */
@SpringBootApplication
public class UserManagerApplication {

    /**
     * Methode principale qui demarre l'application Spring Boot.
     *
     * @param args les arguments de la ligne de commande (non utilisé́s)
     */
    public static void main(String[] args) {
        SpringApplication.run(UserManagerApplication.class, args);
    }
}
