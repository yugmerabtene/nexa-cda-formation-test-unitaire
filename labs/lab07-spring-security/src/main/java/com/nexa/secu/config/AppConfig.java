package com.nexa.secu.config;

import com.nexa.secu.entity.Utilisateur;
import com.nexa.secu.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration applicative centralisant les beans de securite.
 * <p>
 * Cette classe definit :
 * <ul>
 *   <li>Le service de chargement des utilisateurs depuis la base de donnees</li>
 *   <li>Le gestionnaire d'authentification (AuthenticationManager)</li>
 *   <li>L'initialisation des utilisateurs par defaut au demarrage</li>
 * </ul>
 */
@Configuration
public class AppConfig {

    /**
     * Definit le bean {@link UserDetailsService} qui charge un utilisateur
     * depuis la base de donnees via le repository et le transforme en objet
     * utilisable par Spring Security.
     *
     * @param repo le repository d'acces aux utilisateurs en base
     * @return une implementation de {@link UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailsService(UtilisateurRepository repo) {
        return username -> {
            Utilisateur u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));
            return org.springframework.security.core.userdetails.User.builder()
                .username(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole())
                .build();
        };
    }

    /**
     * Expose le {@link AuthenticationManager} gere par Spring Security,
     * necessaire pour l'authentification manuelle dans le controleur
     * de connexion ({@code AuthController}).
     *
     * @param config la configuration d'authentification fournie par Spring
     * @return le gestionnaire d'authentification
     * @throws Exception si la recuperation du gestionnaire echoue
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Initialise les utilisateurs par defaut dans la base H2 au demarrage
     * de l'application, si ceux-ci n'existent pas deja.
     * <p>
     * Deux comptes sont crees :
     * <ul>
     *   <li><b>admin</b> / admin123 &mdash; role ADMIN</li>
     *   <li><b>user</b> / user123 &mdash; role USER</li>
     * </ul>
     * Les mots de passe sont chiffres avec BCrypt avant d'etre persistes.
     *
     * @param repo    le repository d'acces aux utilisateurs
     * @param encoder l'encodeur de mots de passe BCrypt
     * @return un {@link CommandLineRunner} execute au demarrage
     */
    @Bean
    public CommandLineRunner initUsers(UtilisateurRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByUsername("admin")) {
                repo.save(new Utilisateur("admin", encoder.encode("admin123"), "ADMIN"));
            }
            if (!repo.existsByUsername("user")) {
                repo.save(new Utilisateur("user", encoder.encode("user123"), "USER"));
            }
        };
    }
}
