package com.nexa.secu.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controleur REST illustrant le controle d'acces par role sur les ressources.
 * <p>
 * Trois niveaux d'acces sont configures :
 * <ul>
 *   <li><b>Public</b> : accessible sans authentification</li>
 *   <li><b>Administrateur</b> : reserve au role ADMIN</li>
 *   <li><b>Utilisateur</b> : accessible aux roles ADMIN et USER</li>
 * </ul>
 * La securite est appliquee a deux niveaux : regles globales dans
 * {@code SecurityConfig} (par URL et methode HTTP) et annotations
 * {@code @PreAuthorize} au niveau des methodes.
 */
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    /**
     * Endpoint public accessible a tous, sans aucune restriction.
     *
     * @return un message confirmant l'acces public
     */
    @GetMapping("/public")
    public Map<String, String> listePublique() {
        return Map.of("message", "Liste publique des produits accessible à tous");
    }

    /**
     * Endpoint restreint au role ADMIN.
     * <p>
     * L'annotation {@code @PreAuthorize} verifie que l'utilisateur authentifie
     * possede le role {@code ROLE_ADMIN}.
     *
     * @return un message confirmant l'acces administrateur
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> espaceAdmin() {
        return Map.of("message", "Espace administration — réservé aux ADMIN");
    }

    /**
     * Endpoint accessible aux utilisateurs possedant le role ADMIN ou USER.
     * <p>
     * L'annotation {@code @PreAuthorize("hasAnyRole('ADMIN','USER')")}
     * autorise l'acces si l'utilisateur possede au moins un des deux roles.
     *
     * @return un message confirmant l'acces utilisateur
     */
    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Map<String, String> espaceUtilisateur() {
        return Map.of("message", "Espace utilisateur — accessible aux ADMIN et USER");
    }
}
