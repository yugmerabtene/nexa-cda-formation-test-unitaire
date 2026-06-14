package com.nexa.mocking;

/**
 * <h1>UserService — Service métier avec dépendances</h1>
 *
 * <p>
 * C'est la classe que l'on va <b>tester</b>. Elle dépend de :
 * </p>
 * <ul>
 *   <li>{@link UserRepository} — pour l'accès aux données</li>
 *   <li>{@link EmailService} — pour l'envoi de notifications</li>
 * </ul>
 *
 * <p>
 * Ces deux dépendances seront <b>mockées</b> dans les tests pour isoler
 * le comportement de {@code UserService} de ses dépendances.
 * </p>
 */
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * L'injection se fait par constructeur (bonne pratique).
     * Cela permet d'injecter les mocks facilement dans les tests.
     */
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Recherche un utilisateur par son ID.
     * @throws UserNotFoundException si l'utilisateur n'existe pas
     */
    public User trouverParId(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundException("Utilisateur introuvable : id=" + id);
        }
        return user;
    }

    /**
     * Crée un nouvel utilisateur et envoie un email de bienvenue.
     * @throws IllegalArgumentException si l'email existe déjà
     */
    public User creerUtilisateur(String nom, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé : " + email);
        }

        User user = new User(null, nom, email, true);
        User saved = userRepository.save(user);

        emailService.envoyerEmail(email,
            "Bienvenue " + nom + " !",
            "Votre compte a été créé avec succès.");

        return saved;
    }

    /**
     * Désactive un utilisateur (soft delete).
     */
    public void desactiverUtilisateur(Long id) {
        User user = trouverParId(id);
        user.setActif(false);
        userRepository.save(user);
        emailService.envoyerEmail(user.getEmail(),
            "Compte désactivé",
            "Votre compte a été désactivé.");
    }
}
