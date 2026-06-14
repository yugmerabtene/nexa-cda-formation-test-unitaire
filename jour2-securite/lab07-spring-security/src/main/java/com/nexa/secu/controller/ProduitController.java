package com.nexa.secu.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <h1>ProduitController sécurisé</h1>
 *
 * <h2>{@code @PreAuthorize}</h2>
 * <p>
 * Vérifie les droits AVANT d'exécuter la méthode.
 * Si l'utilisateur n'a pas le rôle requis, Spring retourne 403 Forbidden.
 * </p>
 * <p>Syntaxe : {@code hasRole('ADMIN')}, {@code hasAnyRole('ADMIN','USER')},
 * {@code isAuthenticated()}, etc.</p>
 */
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    @GetMapping("/public")
    public Map<String, String> listePublique() {
        return Map.of("message", "Liste publique des produits accessible à tous");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> espaceAdmin() {
        return Map.of("message", "Espace administration — réservé aux ADMIN");
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Map<String, String> espaceUtilisateur() {
        return Map.of("message", "Espace utilisateur — accessible aux ADMIN et USER");
    }
}
