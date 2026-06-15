package com.nexa.springintro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de l'application Spring Boot.
 * Point d'entree qui demarre le contexte Spring et le serveur embarque.
 */
@SpringBootApplication
public class SpringIntroApplication {

    /**
     * Methode principale qui lance l'application.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringIntroApplication.class, args);
    }
}
