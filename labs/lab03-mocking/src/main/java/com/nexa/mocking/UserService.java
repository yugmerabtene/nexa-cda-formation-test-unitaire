package com.nexa.mocking;

public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User trouverParId(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw new UserNotFoundException("Utilisateur introuvable : id=" + id);
        }
        return user;
    }

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

    public void desactiverUtilisateur(Long id) {
        User user = trouverParId(id);
        user.setActif(false);
        userRepository.save(user);
        emailService.envoyerEmail(user.getEmail(),
            "Compte désactivé",
            "Votre compte a été désactivé.");
    }
}
