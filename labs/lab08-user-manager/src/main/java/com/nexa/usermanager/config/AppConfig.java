package com.nexa.usermanager.config;

import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration applicative : services Spring Security et initialisation des donnees.
 *
 * <p>Cette classe definit deux beans principaux :</p>
 * <ul>
 *   <li>{@link UserDetailsService} : service de chargement des utilisateurs pour
 *       l'authentification Spring Security. Il recherche les utilisateurs par email
 *       dans le repository et les convertit en objets {@code UserDetails}.</li>
 *   <li>{@link CommandLineRunner} : initialise la base de donnees avec deux
 *       utilisateurs par defaut (admin et user) si elle est vide au demarrage.</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    /**
     * Definit le service de chargement des utilisateurs pour Spring Security.
     *
     * <p>Ce bean est utilise par le {@code AuthenticationManager} pour charger
     * les details d'un utilisateur lors de l'authentification. Il recherche
     * l'utilisateur par email et construit un objet {@code UserDetails} avec
     * son email, son mot de passe hache et son role.</p>
     *
     * @param repo le repository d'acces aux utilisateurs
     * @return un {@link UserDetailsService} qui charge les utilisateurs par email
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return email -> {
            User u = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
            return org.springframework.security.core.userdetails.User.builder()
                .username(u.getEmail())
                .password(u.getPassword())
                .roles(u.getRole().name())
                .build();
        };
    }

    /**
     * Initialise la base de donnees avec des utilisateurs par defaut au demarrage.
     *
     * <p>Ce runner s'execute au lancement de l'application. Il verifie si les
     * utilisateurs par defaut existent deja (par email) et les cree uniquement
     * s'ils sont absents. Cela evite les doublons lors des redemarrages.</p>
     *
     * <p>Utilisateurs crees :</p>
     * <ul>
     *   <li><b>admin@nexa.fr</b> / admin123 - Role ADMIN</li>
     *   <li><b>user@nexa.fr</b>  / user123  - Role USER</li>
     * </ul>
     *
     * @param repo    le repository d'acces aux utilisateurs
     * @param encoder l'encodeur de mot de passe pour hacher les mots de passe
     * @return un {@link CommandLineRunner} qui initialise les donnees
     */
    @Bean
    public CommandLineRunner init(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByEmail("admin@nexa.fr")) {
                repo.save(new User("Admin", "Nexa", "admin@nexa.fr",
                    encoder.encode("admin123"), User.Role.ADMIN));
            }
            if (!repo.existsByEmail("user@nexa.fr")) {
                repo.save(new User("User", "Simple", "user@nexa.fr",
                    encoder.encode("user123"), User.Role.USER));
            }
        };
    }
}
