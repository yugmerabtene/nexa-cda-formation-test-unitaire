package com.nexa.banque.service;

import com.nexa.banque.model.CompteBancaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TDD : Service de Virement")
class ServiceVirementTest {

    private ServiceVirement service;
    private CompteBancaire compteAlice;
    private CompteBancaire compteBob;

    @BeforeEach
    void setUp() {
        service = new ServiceVirement();
        compteAlice = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
        compteBob = new CompteBancaire(2L, "Bob", new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Virement standard")
    class VirementStandard {

        @Test
        @DisplayName("Le virement débite la source et crédite la destination")
        void virementDebiteSourceCrediteDestination() {
            service.effectuerVirement(compteAlice, compteBob,
                new BigDecimal("200.00"), "Remboursement");

            assertEquals(new BigDecimal("800.00"), compteAlice.getSolde(),
                "Alice doit être débitée de 200€");
            assertEquals(new BigDecimal("700.00"), compteBob.getSolde(),
                "Bob doit être crédité de 200€");
        }

        @Test
        @DisplayName("Le virement crée une transaction chez l'émetteur et le bénéficiaire")
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

        @Test
        @DisplayName("Somme des soldes conservée après virement")
        void sommeSoldesConservee() {
            BigDecimal sommeAvant = compteAlice.getSolde().add(compteBob.getSolde());

            service.effectuerVirement(compteAlice, compteBob,
                new BigDecimal("150.00"), "Test conservation");

            BigDecimal sommeApres = compteAlice.getSolde().add(compteBob.getSolde());
            assertEquals(sommeAvant, sommeApres,
                "La somme totale des soldes doit être conservée");
        }
    }

    @Nested
    @DisplayName("Cas d'erreur")
    class CasErreur {

        @Test
        @DisplayName("Virement vers le même compte interdit")
        void virementMemeCompteInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteAlice,
                    new BigDecimal("100.00"), "Moi-même"));
        }

        @Test
        @DisplayName("Montant nul interdit")
        void montantNulInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    BigDecimal.ZERO, "Zéro"));
        }

        @Test
        @DisplayName("Montant négatif interdit")
        void montantNegatifInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    new BigDecimal("-50.00"), "Négatif"));
        }

        @Test
        @DisplayName("Solde insuffisant chez l'émetteur")
        void soldeInsuffisantEmetteur() {
            assertThrows(IllegalArgumentException.class,
                () -> service.effectuerVirement(compteAlice, compteBob,
                    new BigDecimal("2000.00"), "Trop"));
        }
    }
}
