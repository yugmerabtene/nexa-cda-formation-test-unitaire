package com.nexa.banque.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represente un compte bancaire avec gestion des depots, retraits et virements.
 * <p>
 * Cette classe est thread-safe : toutes les méthodes publiques modifiant ou lisant
 * l'état du compte sont synchronisees sur l'instance. Les montants sont manipules
 * exclusivement avec {@link BigDecimal} pour eviter les erreurs d'arrondi liees aux
 * nombres a virgule flottante.
 * </p>
 *
 * <p>Chaque opération reussie (dépôt, retrait, virement) est enregistree dans un
 * historique de {@link Transaction}. Les identifiants de transaction sont generes
 * de maniere atomique via {@link AtomicLong}.</p>
 *
 * <p>L'historique expose via {@link #getHistorique()} est une copie defensive
 * enveloppee dans une liste non modifiable, garantissant l'encapsulation.</p>
 */
public class CompteBancaire {

    /** Identifiant unique du compte bancaire. */
    private final Long id;

    /** Nom du titulaire du compte. */
    private final String titulaire;

    /** Solde actuel du compte, en euros (represente par {@link BigDecimal}). */
    private BigDecimal solde;

    /** Historique chronologique des transactions effectuees sur le compte. */
    private final List<Transaction> historique;

    /**
     * Generateur atomique d'identifiants de transaction.
     * <p>
     * Initialise a 1 et incremente a chaque nouvelle transaction.
     * L'utilisation d'{@link AtomicLong} garantit l'unicite des IDs même
     * en presence d'accès concurrents.
     * </p>
     */
    private final AtomicLong transactionIdGenerator;

    /**
     * Cree un nouveau compte bancaire avec un solde initial.
     *
     * @param id           identifiant unique du compte (ne peut pas etre null)
     * @param titulaire    nom du titulaire du compte
     * @param soldeInitial solde de depart, doit etre positif ou zero
     * @throws IllegalArgumentException si le solde initial est strictement negatif
     */
    public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) {
        if (soldeInitial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le solde initial ne peut pas etre negatif");
        }
        this.id = id;
        this.titulaire = titulaire;
        this.solde = soldeInitial;
        this.historique = new ArrayList<>();
        this.transactionIdGenerator = new AtomicLong(1);
    }

    /**
     * Retourne l'identifiant du compte.
     *
     * @return l'ID du compte
     */
    public Long getId() { return id; }

    /**
     * Retourne le nom du titulaire du compte.
     *
     * @return le nom du titulaire
     */
    public String getTitulaire() { return titulaire; }

    /**
     * Retourne le solde actuel du compte.
     * <p>
     * La synchronisation garantit la lecture d'un solde coherent
     * même en presence d'ecritures concurrentes.
     * </p>
     *
     * @return le solde actuel (jamais null, jamais negatif)
     */
    public synchronized BigDecimal getSolde() { return solde; }

    /**
     * Effectue un dépôt d'argent sur le compte.
     * <p>
     * Le montant doit etre strictement positif. Le solde est incremente
     * du montant, et une transaction de type {@code DEPOT} est enregistree
     * dans l'historique avec le solde resultant.
     * </p>
     *
     * @param montant     somme a deposer, doit etre strictement positive
     * @param description libelle descriptif de l'opération
     * @throws IllegalArgumentException si le montant est negatif ou egal a zero
     */
    public synchronized void deposer(BigDecimal montant, String description) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du dépôt doit etre strictement positif");
        }
        this.solde = this.solde.add(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.DEPOT, montant, this.solde, description));
    }

    /**
     * Effectue un retrait d'argent depuis le compte.
     * <p>
     * Le montant doit etre strictement positif et ne peut pas depasser
     * le solde actuel. Le solde est decremente du montant, et une transaction
     * de type {@code RETRAIT} est enregistree dans l'historique.
     * </p>
     *
     * @param montant     somme a retirer, doit etre strictement positive et inferieure ou egale au solde
     * @param description libelle descriptif de l'opération
     * @throws IllegalArgumentException si le montant est negatif, nul, ou superieur au solde disponible
     */
    public synchronized void retirer(BigDecimal montant, String description) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du retrait doit etre strictement positif");
        }
        if (montant.compareTo(this.solde) > 0) {
            throw new IllegalArgumentException(
                String.format("Solde insuffisant : demande %s€, disponible %s€", montant, this.solde));
        }
        this.solde = this.solde.subtract(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.RETRAIT, montant, this.solde, description));
    }

    /**
     * Recoit un virement entrant et credite le compte.
     * <p>
     * Contrairement a {@link #deposer(BigDecimal, String)}, cette méthode
     * n'effectue pas de validation du montant (la validation est deleguee
     * au {@code ServiceVirement}). Une transaction de type
     * {@code VIREMENT_RECU} est enregistree.
     * </p>
     *
     * @param montant     somme recue, doit etre positive
     * @param description libelle descriptif du virement (ex: "Virement de Bob : Remboursement")
     */
    public synchronized void recevoirVirement(BigDecimal montant, String description) {
        this.solde = this.solde.add(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.VIREMENT_RECU, montant, this.solde, description));
    }

    /**
     * Emet un virement sortant et debite le compte.
     * <p>
     * Le montant doit etre inferieur ou egal au solde disponible.
     * Une transaction de type {@code VIREMENT_EMIS} est enregistree.
     * </p>
     *
     * @param montant     somme a transferer, doit etre inferieure ou egale au solde
     * @param description libelle descriptif du virement (ex: "Virement vers Alice : Cadeau")
     * @throws IllegalArgumentException si le montant depasse le solde disponible
     */
    public synchronized void emettreVirement(BigDecimal montant, String description) {
        if (montant.compareTo(this.solde) > 0) {
            throw new IllegalArgumentException(
                String.format("Solde insuffisant pour le virement : demande %s€, disponible %s€", montant, this.solde));
        }
        this.solde = this.solde.subtract(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.VIREMENT_EMIS, montant, this.solde, description));
    }

    /**
     * Retourne une copie immuable de l'historique des transactions.
     * <p>
     * La liste retournee est une copie defensive enveloppee dans
     * {@link Collections#unmodifiableList(List)}. Toute tentative de
     * modification (ajout, suppression, remplacement) levera une
     * {@link UnsupportedOperationException}.
     * </p>
     * <p>
     * Cas particuliers :
     * <ul>
     *   <li>Compte neuf sans transaction : retourne une liste vide (non null).</li>
     *   <li>Les transactions sont ordonnees chronologiquement (ordre d'insertion).</li>
     * </ul>
     * </p>
     *
     * @return une liste immuable des transactions, jamais null
     */
    public synchronized List<Transaction> getHistorique() {
        return Collections.unmodifiableList(new ArrayList<>(historique));
    }

    /**
     * Retourne le nombre total de transactions enregistrees sur le compte.
     *
     * @return le nombre de transactions (0 ou plus)
     */
    public synchronized int getNombreTransactions() {
        return historique.size();
    }

    /**
     * Retourne la dernière transaction enregistree, ou null si l'historique est vide.
     * <p>
     * Cas particulier : si aucune opération n'a ete effectuee (compte neuf),
     * cette méthode retourne {@code null}.
     * </p>
     *
     * @return la dernière {@link Transaction}, ou {@code null} si l'historique est vide
     */
    public synchronized Transaction getDerniereTransaction() {
        if (historique.isEmpty()) return null;
        return historique.get(historique.size() - 1);
    }
}
