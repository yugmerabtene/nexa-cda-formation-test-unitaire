package com.nexa.secu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree principal de l'application Spring Boot pour le laboratoire
 * de sécurité avec Spring Security et JWT.
 * <p>
 * Cette classe demarre le contexte Spring, active la configuration automatique
 * et lance l'initialisation des utilisateurs par defaut via {@code CommandLineRunner}.
 */
@SpringBootApplication
public class SpringSecurityApplication {

    /**
     * Methode principale qui lance l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande (non utilisé́s)
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityApplication.class, args);
    }
}
