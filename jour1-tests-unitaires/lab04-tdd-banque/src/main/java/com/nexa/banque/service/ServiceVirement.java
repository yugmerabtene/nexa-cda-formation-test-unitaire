package com.nexa.banque.service;

import com.nexa.banque.model.CompteBancaire;

import java.math.BigDecimal;

/**
 * <h1>ServiceVirement — Gère les virements entre comptes</h1>
 */
public class ServiceVirement {

    /**
     * Effectue un virement entre deux comptes.
     *
     * <p>Les deux opérations (débit et crédit) sont atomiques :
     * si l'une échoue, l'autre n'est pas exécutée.</p>
     *
     * @param source      compte émetteur
     * @param destination compte bénéficiaire
     * @param montant     montant à transférer
     * @param motif       description du virement
     */
    public void effectuerVirement(CompteBancaire source, CompteBancaire destination,
                                  BigDecimal montant, String motif) {
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Impossible de virer vers le même compte");
        }
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du virement doit être strictement positif");
        }

        source.emettreVirement(montant, "Virement vers " + destination.getTitulaire() + " : " + motif);
        destination.recevoirVirement(montant, "Virement de " + source.getTitulaire() + " : " + motif);
    }
}
