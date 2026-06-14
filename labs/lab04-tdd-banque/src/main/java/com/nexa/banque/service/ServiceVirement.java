package com.nexa.banque.service;

import com.nexa.banque.model.CompteBancaire;

import java.math.BigDecimal;

public class ServiceVirement {

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
