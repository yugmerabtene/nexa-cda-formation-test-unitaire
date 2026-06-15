package com.nexa.usermanager.service;

import com.nexa.usermanager.entity.User;
import com.nexa.usermanager.exception.ResourceNotFoundException;
import com.nexa.usermanager.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service metier pour la gestion des utilisateurs.
 *
 * <p>Cette classe contient toute la logique metier liee aux utilisateurs :
 * creation, lecture, mise a jour, suppression, desactivation, recherche
 * et comptage.</p>
 *
 * <p>Les methodes sont transactionnelles (heritees de l'annotation au niveau
 * de la classe) pour garantir la coherence des donnees en base.</p>
 *
 * <p>Les mots de passe sont systematiquement hashes avec BCrypt avant
 * d'etre persistes en base de donnees.</p>
 */
@Service
@Transactional
public class UserService {

    /** Repository d'acces aux donnees utilisateur. */
    private final UserRepository repo;

    /** Encodeur de mot de passe (BCrypt). */
    private final PasswordEncoder encoder;

    /**
     * Constructeur avec injection de dependances.
     *
     * @param repo    le repository utilisateur
     * @param encoder l'encodeur de mot de passe BCrypt
     */
    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    /**
     * Cree un nouvel utilisateur apres verification d'unicite de l'email
     * et hachage du mot de passe.
     *
     * <p>Le mot de passe fourni en clair est hache avec BCrypt avant
     * d'etre stocke en base de donnees.</p>
     *
     * @param user l'utilisateur a creer (avec mot de passe en clair)
     * @return l'utilisateur cree avec son identifiant genere
     * @throws RuntimeException si l'email est deja utilise par un autre utilisateur
     */
    public User creer(User user) {
        if (repo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email deja utilise");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    /**
     * Retourne la liste complete de tous les utilisateurs.
     *
     * @return la liste de tous les utilisateurs en base
     */
    public List<User> listerTous() {
        return repo.findAll();
    }

    /**
     * Retourne une page d'utilisateurs avec pagination.
     *
     * @param pageable les parametres de pagination (numero de page, taille, tri)
     * @return une page contenant les utilisateurs correspondants
     */
    public Page<User> listerPagine(Pageable pageable) {
        return repo.findAll(pageable);
    }

    /**
     * Recherche un utilisateur par son identifiant unique.
     *
     * @param id l'identifiant de l'utilisateur recherche
     * @return l'utilisateur trouve
     * @throws ResourceNotFoundException si aucun utilisateur ne correspond a cet ID
     */
    public User trouverParId(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : id=" + id));
    }

    /**
     * Recherche un utilisateur par son adresse email.
     *
     * @param email l'adresse email a rechercher
     * @return un {@link Optional} contenant l'utilisateur s'il existe
     */
    public Optional<User> trouverParEmail(String email) {
        return repo.findByEmail(email);
    }

    /**
     * Met a jour un utilisateur existant avec les nouvelles valeurs fournies.
     *
     * <p>Le mot de passe n'est modifie que si la nouvelle valeur est non nulle
     * et non vide. Cela permet une mise a jour partielle sans changer le mot
     * de passe.</p>
     *
     * <p>La date de modification est automatiquement mise a jour a l'instant
     * present.</p>
     *
     * @param id     l'identifiant de l'utilisateur a mettre a jour
     * @param update l'objet contenant les nouvelles valeurs
     * @return l'utilisateur mis a jour
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public User mettreAJour(Long id, User update) {
        User existant = trouverParId(id);
        existant.setNom(update.getNom());
        existant.setPrenom(update.getPrenom());
        existant.setEmail(update.getEmail());
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existant.setPassword(encoder.encode(update.getPassword()));
        }
        existant.setRole(update.getRole());
        existant.setActif(update.isActif());
        existant.setDateModification(LocalDateTime.now());
        return repo.save(existant);
    }

    /**
     * Supprime definitivement un utilisateur de la base de donnees.
     *
     * @param id l'identifiant de l'utilisateur a supprimer
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public void supprimer(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable : id=" + id);
        }
        repo.deleteById(id);
    }

    /**
     * Desactive un utilisateur sans le supprimer (suppression logique).
     *
     * <p>Le compte est conserve en base mais marque comme inactif,
     * empechant son utilisation jusqu'a reactivation eventuelle.</p>
     *
     * @param id l'identifiant de l'utilisateur a desactiver
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    public void desactiver(Long id) {
        User u = trouverParId(id);
        u.setActif(false);
        repo.save(u);
    }

    /**
     * Recherche des utilisateurs par nom partiel, insensible a la casse.
     *
     * @param nom la chaine de recherche partielle sur le nom
     * @return la liste des utilisateurs dont le nom contient la chaine
     */
    public List<User> rechercherParNom(String nom) {
        return repo.findByNomContainingIgnoreCase(nom);
    }

    /**
     * Retourne la liste des utilisateurs actifs.
     *
     * @return la liste des utilisateurs dont le compte est actif
     */
    public List<User> listerActifs() {
        return repo.findByActif(true);
    }

    /**
     * Compte le nombre d'utilisateurs ayant un role specifique.
     *
     * @param role le role pour lequel compter les utilisateurs (USER ou ADMIN)
     * @return le nombre d'utilisateurs avec ce role
     */
    public long compterParRole(User.Role role) {
        return repo.countByRole(role);
    }
}
