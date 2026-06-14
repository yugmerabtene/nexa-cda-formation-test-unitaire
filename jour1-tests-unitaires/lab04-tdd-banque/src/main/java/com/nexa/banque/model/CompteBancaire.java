package com.nexa.banque.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h1>CompteBancaire — Compte avec solde et historique</h1>
 *
 * <p>Thread-safe via synchronisation sur le verrou interne.</p>
 */
public class CompteBancaire {

    private final Long id;
    private final String titulaire;
    private BigDecimal solde;
    private final List<Transaction> historique;
    private final AtomicLong transactionIdGenerator;

    public CompteBancaire(Long id, String titulaire, BigDecimal soldeInitial) {
        if (soldeInitial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le solde initial ne peut pas être négatif");
        }
        this.id = id;
        this.titulaire = titulaire;
        this.solde = soldeInitial;
        this.historique = new ArrayList<>();
        this.transactionIdGenerator = new AtomicLong(1);
    }

    public Long getId() { return id; }
    public String getTitulaire() { return titulaire; }
    public BigDecimal getSolde() { return solde; }

    public synchronized void deposer(BigDecimal montant, String description) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du dépôt doit être strictement positif");
        }
        this.solde = this.solde.add(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.DEPOT, montant, this.solde, description));
    }

    public synchronized void retirer(BigDecimal montant, String description) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du retrait doit être strictement positif");
        }
        if (montant.compareTo(this.solde) > 0) {
            throw new IllegalArgumentException(
                String.format("Solde insuffisant : demande %s€, disponible %s€", montant, this.solde));
        }
        this.solde = this.solde.subtract(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.RETRAIT, montant, this.solde, description));
    }

    public synchronized void recevoirVirement(BigDecimal montant, String description) {
        this.solde = this.solde.add(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.VIREMENT_RECU, montant, this.solde, description));
    }

    public synchronized void emettreVirement(BigDecimal montant, String description) {
        if (montant.compareTo(this.solde) > 0) {
            throw new IllegalArgumentException(
                String.format("Solde insuffisant pour le virement : demande %s€, disponible %s€", montant, this.solde));
        }
        this.solde = this.solde.subtract(montant);
        historique.add(new Transaction(transactionIdGenerator.getAndIncrement(),
            Transaction.Type.VIREMENT_EMIS, montant, this.solde, description));
    }

    public synchronized List<Transaction> getHistorique() {
        return Collections.unmodifiableList(new ArrayList<>(historique));
    }

    public synchronized int getNombreTransactions() {
        return historique.size();
    }

    public synchronized Transaction getDerniereTransaction() {
        if (historique.isEmpty()) return null;
        return historique.get(historique.size() - 1);
    }
}
