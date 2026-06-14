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

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService service;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public UserController(UserService service, JwtUtil jwtUtil, AuthenticationManager authManager) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

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

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> lister() {
        return service.listerTous().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/users/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Page<UserResponse> listerPagine(Pageable pageable) {
        return service.listerPagine(pageable).map(UserResponse::from);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponse> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(service.trouverParId(id)));
    }

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

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @GetMapping("/users/recherche")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> rechercher(@RequestParam String nom) {
        return service.rechercherParNom(nom).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/users/actifs")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<UserResponse> actifs() {
        return service.listerActifs().stream().map(UserResponse::from).toList();
    }
}
