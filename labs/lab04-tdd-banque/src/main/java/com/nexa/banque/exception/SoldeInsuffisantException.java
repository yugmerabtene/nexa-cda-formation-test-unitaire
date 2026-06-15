package com.nexa.banque.exception;

/**
 * Exception metier levee lorsqu'une opération bancaire est tente avec un solde insuffisant.
 * <p>
 * Herite de {@link RuntimeException} (exception non controlee) pour permettre
 * une propagation simplifiee sans obligation de {@code try/catch} ou de clause {@code throws}.
 * </p>
 *
 * <p>Cas typiques de declenchement :</p>
 * <ul>
 *   <li>Retrait d'un montant superieur au solde disponible</li>
 *   <li>Emission d'un virement dont le montant excede le solde du compte emetteur</li>
 * </ul>
 *
 * <p>
 * Note : dans cette version du code, les validations de solde utilisent
 * {@link IllegalArgumentException}. Cette exception {@code SoldeInsuffisantException}
 * est fournie comme alternative metier plus explicite pour les evolutions futures.
 * </p>
 */
public class SoldeInsuffisantException extends RuntimeException {

    /**
     * Construit une nouvelle exception avec un message descriptif.
     *
     * @param message description de la cause de l'exception (ex: montant demande vs solde disponible)
     */
    public SoldeInsuffisantException(String message) {
        super(message);
    }
}
