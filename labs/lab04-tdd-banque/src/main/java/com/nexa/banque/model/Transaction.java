package com.nexa.banque.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represente une transaction bancaire enregistree dans l'historique d'un compte.
 * <p>
 * Chaque transaction est immuable après sa creation : elle capture l'état du compte
 * (solde après opération) au moment de l'opération. Les types de transactions sont
 * definis par l'enum interne {@link Type}.
 * </p>
 *
 * <p>Deux transactions sont considerees egales si elles partagent le même identifiant.</p>
 */
public class Transaction {

    /**
     * Enumeration des types d'operations bancaires possibles.
     * <ul>
     *   <li>{@code DEPOT} : dépôt d'argent sur le compte</li>
     *   <li>{@code RETRAIT} : retrait d'argent depuis le compte</li>
     *   <li>{@code VIREMENT_EMIS} : virement sortant du compte</li>
     *   <li>{@code VIREMENT_RECU} : virement entrant sur le compte</li>
     * </ul>
     */
    public enum Type { DEPOT, RETRAIT, VIREMENT_EMIS, VIREMENT_RECU }

    /** Identifiant unique de la transaction, genere par le compte proprietaire. */
    private final Long id;

    /** Type de l'opération (dépôt, retrait, virement emis ou reçu). */
    private final Type type;

    /** Montant de l'opération, toujours strictement positif en valeur absolue. */
    private final BigDecimal montant;

    /** Solde du compte immediatement après l'exécution de l'opération. */
    private final BigDecimal soldeApresOperation;

    /** Description libre de l'opération (motif, reference, etc.). */
    private final String description;

    /** Horodatage de la creation de la transaction (instant de l'opération). */
    private final LocalDateTime dateHeure;

    /**
     * Construit une transaction bancaire immuable.
     * <p>
     * La date et l'heure sont automatiquement initialisees a l'instant de la creation
     * via {@link LocalDateTime#now()}. Le soldeApresOperation reflete l'état du compte
     * après application du montant.
     * </p>
     *
     * @param id                   identifiant unique de la transaction (genere par {@code AtomicLong})
     * @param type                 type d'opération ({@link Type})
     * @param montant              montant positif de l'opération (valeur absolue)
     * @param soldeApresOperation  solde du compte après exécution de l'opération
     * @param description          libelle descriptif de l'opération (ne peut pas etre null)
     */
    public Transaction(Long id, Type type, BigDecimal montant,
                       BigDecimal soldeApresOperation, String description) {
        this.id = id;
        this.type = type;
        this.montant = montant;
        this.soldeApresOperation = soldeApresOperation;
        this.description = description;
        this.dateHeure = LocalDateTime.now();
    }

    /**
     * Retourne l'identifiant unique de la transaction.
     *
     * @return l'ID de la transaction
     */
    public Long getId() { return id; }

    /**
     * Retourne le type d'opération de la transaction.
     *
     * @return le {@link Type} (DEPOT, RETRAIT, VIREMENT_EMIS, VIREMENT_RECU)
     */
    public Type getType() { return type; }

    /**
     * Retourne le montant de l'opération en valeur absolue.
     *
     * @return le montant positif de la transaction
     */
    public BigDecimal getMontant() { return montant; }

    /**
     * Retourne le solde du compte après exécution de cette opération.
     *
     * @return le solde resultant, tel qu'il etait immediatement après la transaction
     */
    public BigDecimal getSoldeApresOperation() { return soldeApresOperation; }

    /**
     * Retourne la description libre associee a la transaction.
     *
     * @return le libelle descriptif (motif, reference, etc.)
     */
    public String getDescription() { return description; }

    /**
     * Retourne la date et l'heure exactes de la creation de la transaction.
     *
     * @return le {@link LocalDateTime} de l'opération
     */
    public LocalDateTime getDateHeure() { return dateHeure; }

    /**
     * Compare deux transactions par leur identifiant.
     * <p>
     * Deux transactions sont egales si et seulement si elles ont le même ID.
     * Les autres champs (type, montant, date, etc.) ne sont pas pris en compte.
     * </p>
     *
     * @param o l'objet a comparer
     * @return {@code true} si les deux transactions ont le même ID, {@code false} sinon
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id);
    }

    /**
     * Calcule le code de hachage base uniquement sur l'identifiant.
     *
     * @return le hash code de la transaction
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Retourne une representation textuelle de la transaction.
     * <p>Format : {@code [dateHeure] TYPE : montant€ (solde: soldeApresOperation€) - description}</p>
     *
     * @return la chaine formatee decrivant la transaction
     */
    @Override
    public String toString() {
        return String.format("[%s] %s : %s€ (solde: %s€) - %s",
            dateHeure, type, montant, soldeApresOperation, description);
    }
}
