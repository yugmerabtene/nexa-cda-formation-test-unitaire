package com.nexa.banque.service;

import com.nexa.banque.model.CompteBancaire;

import java.math.BigDecimal;

/**
 * Service metier orchestrant les virements entre deux comptes bancaires.
 * <p>
 * Ce service validé les paramètrès du virement (comptes distincts, montant positif)
 * puis delegue les operations de debit et credit aux méthodes specialisees
 * de {@link CompteBancaire} : {@code emettreVirement} sur le compte source et
 * {@code recevoirVirement} sur le compte destination.
 * </p>
 *
 * <p>
 * La sécurité des threads est assuree par la synchronisation interne de
 * {@link CompteBancaire}. Le service lui-même est sans état (stateless)
 * et donc thread-safe par construction.
 * </p>
 *
 * <p>Regles metier appliquees :</p>
 * <ul>
 *   <li>Les comptes source et destination doivent etre distincts.</li>
 *   <li>Le montant doit etre strictement positif.</li>
 *   <li>Le solde du compte source doit etre suffisant (validé par {@code emettreVirement}).</li>
 * </ul>
 */
public class ServiceVirement {

    /**
     * Effectue un virement d'un compte source vers un compte destination.
     * <p>
     * Le montant est debite du compte source via {@link CompteBancaire#emettreVirement}
     * puis credite sur le compte destination via {@link CompteBancaire#recevoirVirement}.
     * Les deux operations sont enregistrees dans l'historique de chaque compte
     * avec une description incluant le nom du titulaire de l'autre compte et le motif.
     * </p>
     *
     * <p>
     * Cas d'erreur : si l'une des validations échoué, aucune ecriture n'a lieu
     * car les méthodes de {@code CompteBancaire} valident avant de modifier le solde.
     * </p>
     *
     * @param source      compte emetteur du virement (sera debite)
     * @param destination compte beneficiaire du virement (sera credite)
     * @param montant     somme a transferer, doit etre strictement positive
     * @param motif       raison du virement (ex: "Remboursement", "Cadeau")
     * @throws IllegalArgumentException si les comptes sont identiques
     * @throws IllegalArgumentException si le montant est negatif ou nul
     * @throws IllegalArgumentException si le solde du compte source est insuffisant
     *                                  (propagation de l'exception levee par {@code emettreVirement})
     */
    public void effectuerVirement(CompteBancaire source, CompteBancaire destination,
                                  BigDecimal montant, String motif) {
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Impossible de virer vers le même compte");
        }
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant du virement doit etre strictement positif");
        }

        source.emettreVirement(montant, "Virement vers " + destination.getTitulaire() + " : " + motif);
        destination.recevoirVirement(montant, "Virement de " + source.getTitulaire() + " : " + motif);
    }
}
