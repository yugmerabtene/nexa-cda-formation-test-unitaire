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
 * Configuration des beans Spring Security et initialisation des utilisateurs de test.
 */
@Configuration
public class AppConfig {

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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

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
