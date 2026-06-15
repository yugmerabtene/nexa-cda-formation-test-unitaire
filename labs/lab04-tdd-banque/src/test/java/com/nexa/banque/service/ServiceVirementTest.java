package com.nexa.banque.service;

import com.nexa.banque.model.CompteBancaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service {@link ServiceVirement}.
 * <p>
 * Verifie le comportement du virement entre deux comptes : debit de la source,
 * credit de la destination, creation des transactions, conservation de la masse
 * monetaire, et rejet des paramètrès invalides.
 * </p>
 *
 * <p>Organisation via {@link Nested} :</p>
 * <ul>
 *   <li>Virement standard : cas nominaux</li>
 *   <li>Cas d'erreur : validations des paramètrès</li>
 * </ul>
 */
@DisplayName("TDD : Service de Virement")
class ServiceVirementTest {

    /** Instance du service de virement, reinitialisee avant chaque test. */
    private ServiceVirement service;

    /** Compte d'Alice, initialise avec 1000.00€, reinitialise avant chaque test. */
    private CompteBancaire compteAlice;

    /** Compte de Bob, initialise avec 500.00€, reinitialise avant chaque test. */
    private CompteBancaire compteBob;

    /**
     * Initialise les fixtures avant chaque test.
     * <p>
     * Cree une instance fraiche de {@link ServiceVirement} et deux comptes
     * (Alice avec 1000.00€ et Bob avec 500.00€) pour garantir l'isolation
     * entre les tests.
     * </p>
     */
    @BeforeEach
    void setUp() {
        service = new ServiceVirement();
        compteAlice = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
        compteBob = new CompteBancaire(2L, "Bob", new BigDecimal("500.00"));
    }

    /**
     * Tests des virements standard (cas nominaux).
     * <p>Verifie le comportement normal d'un virement : debit/credit corrects,
     * creation de transactions, et conservation de la somme des soldes.</p>
     */
    @Nested
    @DisplayName("Virement standard")
    class VirementStandard {

        /**
         * Verifie qu'un virement debite correctement la source et credite la destination.
         * <p>
         * Alice (1000€) vire 200€ a Bob (500€).
         * Resultat attendu : Alice = 800€, Bob = 700€.
         * </p>
         */
        @Test
        @DisplayName("Le virement debite la source et credite la destination")
        void virementDebiteSourceCrediteDestination() {
            service.effectuerVirement(compteAlice, compteBob,
                new BigDecimal("200.00"), "Remboursement");

            assertEquals(new BigDecimal("800.00"), compteAlice.getSolde(),
                "Alice doit etre debitee de 200€");
            assertEquals(new BigDecimal("700.00"), compteBob.getSolde(),
                "Bob doit etre credite de 200€");
        }

        /**
         * Verifie qu'un virement créé une transaction chez l'emetteur et le beneficiaire.
         * <p>
         * Le compte source reçoit une transaction {@code VIREMENT_EMIS} et
         * le compte destination reçoit une transaction {@code VIREMENT_RECU}.
         * Chaque compte doit avoir exactement 1 transaction après le virement.
         * </p>
         */
        @Test
        @DisplayName("Le virement créé une transaction chez l'emetteur et le beneficiaire")
        void virementCreeTransactions() {
            service.effectuerVirement(compteAlice, compteBob,
                new BigDecimal("100.00"), "Cadeau");

            assertEquals(1, compteAlice.getNombreTransactions());
            assertEquals(1, compteBob.getNombreTransactions());
            assertEquals(
                com.nexa.banque.model.Transaction.Type.VIREMENT_EMIS,
                compteAlice.getDerniereTransaction().getType());
            assertEquals(
                com.nexa.banque.model.Transaction.Type.VIREMENT_RECU,
                compteBob.getDerniereTransaction().getType());
        }

        /**
         * Verifie que la somme des soldes des deux comptes est conservee après virement.
         * <p>
         * Propriete d'invariance : la masse monetaire totale (somme des soldes)
         * doit etre identique avant et après le virement. Aucune creation ni
         * destruction d'argent.
         * </p>
         */
        @Test
        @DisplayName("Somme des soldes conservee après virement")
        void sommeSoldesConservee() {
            BigDecimal sommeAvant = compteAlice.getSolde().add(compteBob.getSolde());

            service.effectuerVirement(compteAlice, compteBob,
                new BigDecimal("150.00"), "Test conservation");

            BigDecimal sommeApres = compteAlice.getSolde().add(compteBob.getSolde());
            assertEquals(sommeAvant, sommeApres,
                "La somme totale des soldes doit etre conservee");
        }
    }

    /**
     * Tests des cas d'erreur lors d'un virement.
     * <p>Verifie que les paramètrès invalides (même compte, montant nul/negatif,
     * solde insuffisant) sont correctement rejetes avec une exception.</p>
     */
    @Nested
    @DisplayName("Cas d'erreur")
    class CasErreur {

        /**
         * Verifie qu'un virement d'un compte vers lui-même est interdit.
         * <p>Cas d'erreur : source et destination identiques. Cela n'a pas de sens
         * metier et pourrait creer des incoherences.</p>
         */
        @Test
        @DisplayName("Virement vers le même compte interdit")
        void virementMemeCompteInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteAlice,
                    new BigDecimal("100.00"), "Moi-même"));
        }

        /**
         * Verifie qu'un montant nul est interdit.
         * <p>Cas d'erreur : un virement de 0€ n'a pas de sens metier.</p>
         */
        @Test
        @DisplayName("Montant nul interdit")
        void montantNulInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    BigDecimal.ZERO, "Zero"));
        }

        /**
         * Verifie qu'un montant negatif est interdit.
         * <p>Cas d'erreur : un montant negatif inverserait le sens du virement.</p>
         */
        @Test
        @DisplayName("Montant negatif interdit")
        void montantNegatifInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    new BigDecimal("-50.00"), "Negatif"));
        }

        /**
         * Verifie qu'un virement est bloque si le solde de l'emetteur est insuffisant.
         * <p>
         * Alice a 1000€, elle tente de virer 2000€ a Bob.
         * L'exception est levee par {@code CompteBancaire.emettreVirement()}
         * et propagee par le service.
         * </p>
         */
        @Test
        @DisplayName("Solde insuffisant chez l'emetteur")
        void soldeInsuffisantEmetteur() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    new BigDecimal("2000.00"), "Trop"));
        }
    }
}
