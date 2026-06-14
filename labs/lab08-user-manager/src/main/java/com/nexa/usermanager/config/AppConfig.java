package com.nexa.usermanager.config;

import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {

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
