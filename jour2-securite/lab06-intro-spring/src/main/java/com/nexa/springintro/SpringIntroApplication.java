package com.nexa.springintro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <h1>@SpringBootApplication</h1>
 * <p>
 * Cette annotation combine trois annotations :
 * </p>
 * <ul>
 *   <li>{@code @Configuration} — la classe peut définir des beans Spring</li>
 *   <li>{@code @EnableAutoConfiguration} — Spring Boot configure automatiquement
 *       l'application selon les dépendances présentes (ex: datasource si JPA est là)</li>
 *   <li>{@code @ComponentScan} — scanne les packages pour trouver
 *       les composants Spring (@Service, @Controller, @Repository...)</li>
 * </ul>
 */
@SpringBootApplication
public class SpringIntroApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringIntroApplication.class, args);
    }
}
