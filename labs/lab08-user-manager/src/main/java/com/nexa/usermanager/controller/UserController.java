package com.nexa.usermanager.controller;

import com.nexa.usermanager.dto.UserRequest;
import com.nexa.usermanager.dto.UserResponse;
import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.security.JwtUtil;
import com.nexa.usermanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controleur REST principal exposant les endpoints de l'API de gestion d'utilisateurs.
 *
 * <p>Ce controleur gere l'authentification JWT et les operations CRUD sur les
 * utilisateurs. L'acces aux endpoints est protege par Spring Security avec
 * des annotations {@link PreAuthorize} qui restreignent l'acces selon les roles.</p>
 *
 * <p>Tous les endpoints sont prefixes par {@code /api}.</p>
 *
 * <p>Roles d'acces :</p>
 * <ul>
 *   <li><b>Public</b> : {@code POST /api/auth/login}, {@code GET /actuator/health}</li>
 *   <li><b>ADMIN et USER</b> : tous les {@code GET} en lecture</li>
 *   <li><b>ADMIN uniquement</b> : {@code POST}, {@code PUT}, {@code DELETE}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class UserController {

    /** Service metier de gestion des utilisateurs. */
    private final UserService service;

    /** Utilitaire de generation et validation des tokens JWT. */
    private final JwtUtil jwtUtil;

    /** Gestionnaire d'authentification Spring Security. */
    private final AuthenticationManager authManager;

    /**
     * Constructeur avec injection de dependances.
     *
     * @param service     le service utilisateur
     * @param jwtUtil     l'utilitaire JWT
     * @param authManager le gestionnaire d'authentification
     */
    public UserController(UserService service, JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

    /**
     * Endpoint d'authentification : verifie les identifiants et retourne un token JWT.
     *
     * <p>Le corps de la requete doit contenir un objet JSON avec les champs
     * {@code email} et {@code password}.</p>
     *
     * @param body le corps de la requete contenant email et password
     * @return un token JWT en cas de succes, ou une erreur 401 en cas d'echec
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            User user = service.trouverParEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
            return ResponseEntity.ok(Map.of("token", jwtUtil.genererToken(email, user.getRole().name())));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Identifiants invalides"));
        }
    }

    /**
     * Liste tous les utilisateurs du systeme.
     *
     * <p>Accessible aux roles ADMIN et USER.</p>
     *
     * @return la liste des utilisateurs sous forme de DTOs {@link UserResponse}
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> lister() {
        return service.listerTous().stream().map(UserResponse::from).toList();
    }

    /**
     * Liste les utilisateurs avec pagination.
     *
     * <p>Les parametres de pagination (page, size, sort) sont passes
     * en parametres de requete ({@code ?page=0&size=10&sort=nom,asc}).</p>
     *
     * <p>Accessible aux roles ADMIN et USER.</p>
     *
     * @param pageable les parametres de pagination
     * @return une page de DTOs {@link UserResponse}
     */
    @GetMapping("/users/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Page<UserResponse> listerPagine(Pageable pageable) {
        return service.listerPagine(pageable).map(UserResponse::from);
    }

    /**
     * Recherche un utilisateur par son identifiant.
     *
     * <p>Accessible aux roles ADMIN et USER.</p>
     *
     * @param id l'identifiant de l'utilisateur recherche
     * @return le DTO {@link UserResponse} correspondant
     * @throws com.nexa.usermanager.exception.ResourceNotFoundException si l'ID n'existe pas
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponse> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(service.trouverParId(id)));
    }

    /**
     * Cree un nouvel utilisateur.
     *
     * <p>Reserve au role ADMIN. Retourne un statut HTTP 201 Created en cas de succes.</p>
     *
     * <p>Le role fourni dans la requete est converti en majuscules pour
     * correspondre aux valeurs de l'enum {@link User.Role}.</p>
     *
     * @param req le DTO contenant les donnees du nouvel utilisateur (valide)
     * @return le DTO {@link UserResponse} de l'utilisateur cree
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse creer(@Valid @RequestBody UserRequest req) {
        User user = new User();
        user.setNom(req.getNom());
        user.setPrenom(req.getPrenom());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());
        user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        return UserResponse.from(service.creer(user));
    }

    /**
     * Met a jour un utilisateur existant.
     *
     * <p>Reserve au role ADMIN. Tous les champs sont mis a jour, y compris le
     * mot de passe si fourni.</p>
     *
     * @param id  l'identifiant de l'utilisateur a modifier
     * @param req le DTO contenant les nouvelles donnees (valide)
     * @return le DTO {@link UserResponse} de l'utilisateur mis a jour
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse mettreAJour(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        User update = new User();
        update.setNom(req.getNom());
        update.setPrenom(req.getPrenom());
        update.setEmail(req.getEmail());
        update.setPassword(req.getPassword());
        update.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        return UserResponse.from(service.mettreAJour(id, update));
    }

    /**
     * Supprime un utilisateur.
     *
     * <p>Reserve au role ADMIN. Retourne un statut HTTP 204 No Content en cas de succes.</p>
     *
     * @param id l'identifiant de l'utilisateur a supprimer
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    /**
     * Recherche des utilisateurs par nom partiel.
     *
     * <p>La recherche est insensible a la casse et retourne tous les utilisateurs
     * dont le nom contient la chaine fournie en parametre.</p>
     *
     * <p>Accessible aux roles ADMIN et USER.</p>
     *
     * @param nom la chaine de recherche sur le nom
     * @return la liste des utilisateurs correspondants
     */
    @GetMapping("/users/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> rechercher(@RequestParam String nom) {
        return service.rechercherParNom(nom).stream().map(UserResponse::from).toList();
    }

    /**
     * Liste uniquement les utilisateurs actifs.
     *
     * <p>Accessible aux roles ADMIN et USER.</p>
     *
     * @return la liste des utilisateurs dont le compte est actif
     */
    @GetMapping("/users/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> actifs() {
        return service.listerActifs().stream().map(UserResponse::from).toList();
    }
}
