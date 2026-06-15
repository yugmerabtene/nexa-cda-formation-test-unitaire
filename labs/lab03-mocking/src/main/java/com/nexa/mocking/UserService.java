package com.nexa.mocking;

/**
 * Service metier de gestion des utilisateurs.
 *
 * Ce service depend de deux interfaces :
 * - UserRepository : pour l'accès aux donnees (lecture/ecriture en base)
 * - EmailService : pour l'envoi d'emails de notification
 *
 * Ces dépendances sont injectees via le constructeur (Injection de Dependances).
 * Dans les tests, elles seront remplacees par des mocks Mockito pour isoler
 * la logique metier et eviter les accès reels a une base de donnees ou un serveur SMTP.
 *
 * Pattern : cette classe est le SUT (System Under Test) dans UserServiceTest.
 *
 * @see UserRepository
 * @see EmailService
 */
public class UserService {

    /** Acces aux donnees — sera mocke dans les tests */
    private final UserRepository userRepository;

    /** Service d'envoi d'emails — sera mocke dans les tests */
    private final EmailService emailService;

    /**
     * Constructeur avec injection de dépendances.
     *
     * @param userRepository le repository d'utilisateurs (non null)
     * @param emailService le service d'envoi d'emails (non null)
     */
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Recherche un utilisateur par son identifiant.
     *
     * @param id l'identifiant de l'utilisateur
     * @return l'utilisateur trouve
     * @throws UserNotFoundException si aucun utilisateur ne correspond a cet ID
     */
    public User trouverParId(Long id) {
        // Delegue la recherche au repository (mocke en test)
        User user = userRepository.findById(id);

        // Si le repository retourne null, l'utilisateur n'existe pas
        if (user == null) {
            throw new UserNotFoundException("Utilisateur introuvable : id=" + id);
        }
        return user;
    }

    /**
     * Cree un nouvel utilisateur et envoie un email de bienvenue.
     *
     * Algorithme :
     * 1. Verifier que l'email n'est pas déjà utilisé (unicite)
     * 2. Creer un nouvel objet User (actif par defaut)
     * 3. Sauvegarder en base (via le repository)
     * 4. Envoyer un email de bienvenue
     *
     * @param nom le nom de l'utilisateur
     * @param email l'adresse email (doit etre unique)
     * @return l'utilisateur créé et sauvegarde (avec son ID)
     * @throws IllegalArgumentException si l'email est déjà utilisé
     */
    public User creerUtilisateur(String nom, String email) {
        // Verification d'unicite de l'email
        // Si l'email existe déjà, on refuse la creation
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Cet email est déjà utilisé : " + email);
        }

        // Creation de l'utilisateur avec l'ID null (sera attribue par la base)
        User user = new User(null, nom, email, true);

        // Sauvegarde en base — le mock retournera l'utilisateur avec un ID
        User saved = userRepository.save(user);

        // Envoi d'un email de bienvenue (mocke en test)
        emailService.envoyerEmail(email,
            "Bienvenue " + nom + " !",
            "Votre compte a ete créé avec succès.");

        return saved;
    }

    /**
     * Desactive le compte d'un utilisateur.
     *
     * Algorithme :
     * 1. Rechercher l'utilisateur par ID (lève exception si introuvable)
     * 2. Passer le flag actif a false
     * 3. Sauvegarder la modification
     * 4. Envoyer un email de notification
     *
     * @param id l'identifiant de l'utilisateur a désactiver
     * @throws UserNotFoundException si l'utilisateur n'existe pas
     */
    public void desactiverUtilisateur(Long id) {
        // Reutilise trouverParId() pour la recherche et la gestion d'erreur
        User user = trouverParId(id);

        // Modification de l'état
        user.setActif(false);

        // Sauvegarde de la modification en base
        userRepository.save(user);

        // Notification par email
        emailService.envoyerEmail(user.getEmail(),
            "Compte désactivé",
            "Votre compte a ete désactivé.");
    }
}
