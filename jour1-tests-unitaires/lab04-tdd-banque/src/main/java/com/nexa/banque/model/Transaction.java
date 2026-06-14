package com.nexa.banque.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <h1>Transaction — Opération sur un compte bancaire</h1>
 *
 * <p>Immutable (ses champs ne changent pas après construction).
 * Chaque transaction est horodatée et typée.</p>
 */
public class Transaction {

    public enum Type { DEPOT, RETRAIT, VIREMENT_EMIS, VIREMENT_RECU }

    private final Long id;
    private final Type type;
    private final BigDecimal montant;
    private final BigDecimal soldeApresOperation;
    private final String description;
    private final LocalDateTime dateHeure;

    public Transaction(Long id, Type type, BigDecimal montant,
                       BigDecimal soldeApresOperation, String description) {
        this.id = id;
        this.type = type;
        this.montant = montant;
        this.soldeApresOperation = soldeApresOperation;
        this.description = description;
        this.dateHeure = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public BigDecimal getMontant() { return montant; }
    public BigDecimal getSoldeApresOperation() { return soldeApresOperation; }
    public String getDescription() { return description; }
    public LocalDateTime getDateHeure() { return dateHeure; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s : %s€ (solde: %s€) - %s",
            dateHeure, type, montant, soldeApresOperation, description);
    }
}
