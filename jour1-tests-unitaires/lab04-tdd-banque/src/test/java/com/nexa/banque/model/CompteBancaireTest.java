package com.nexa.banque.model;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>TDD : Tests de la classe CompteBancaire</h1>
 *
 * <h2>Annotation {@code @Nested}</h2>
 * <p>
 * Permet d'organiser les tests en <b>groupes hiérarchiques</b>.
 * Chaque classe interne annotée {@code @Nested} contient des tests
 * regroupés par fonctionnalité ou scénario.
 * </p>
 *
 * <h3>Avantages</h3>
 * <ul>
 *   <li>Structure claire dans les rapports de test</li>
 *   <li>Possibilité d'avoir un {@code @BeforeEach} spécifique à chaque groupe</li>
 *   <li>Les {@code @BeforeEach} des classes parentes s'exécutent AVANT ceux des enfants</li>
 * </ul>
 *
 * <h2>Annotation {@code @Tag}</h2>
 * <p>
 * Catégorise les tests pour pouvoir les filtrer à l'exécution :
 * {@code mvn test -Dgroups="unitaire"} ou {@code -DexcludedGroups="lent"}.
 * </p>
 *
 * <h2>Annotation {@code @Timeout}</h2>
 * <p>
 * Fait échouer le test s'il dépasse la durée spécifiée.
 * Protège contre les boucles infinies et les deadlocks.
 * </p>
 *
 * <h2>Annotation {@code @RepeatedTest(n)}</h2>
 * <p>
 * Exécute le test n fois. Utile pour vérifier la stabilité
 * et l'absence d'état résiduel entre les exécutions.
 * </p>
 */
@DisplayName("TDD : Compte Bancaire")
class CompteBancaireTest {

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 1 : Création d'un compte (RED → GREEN → REFACTOR)
     *
     * TDD RED   : Écrire le test AVANT le code. Le test doit échouer
     *             car la classe/méthode n'existe pas encore.
     * TDD GREEN : Écrire le MINIMUM de code pour que le test passe.
     *             Pas de suroptimisation, pas de code superflu.
     * TDD REFACTOR : Améliorer le code sans casser les tests existants.
     *                Renommer, extraire des méthodes, optimiser.
     *
     * Chaque section ci-dessous correspond à une itération TDD.
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Création du compte")
    class CreationCompte {

        @Test
        @DisplayName("Un compte est créé avec un solde initial correct")
        void creationAvecSoldeInitial() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

            assertEquals(1L, compte.getId());
            assertEquals("Alice", compte.getTitulaire());
            assertEquals(new BigDecimal("1000.00"), compte.getSolde());
        }

        @Test
        @DisplayName("Un compte peut être créé avec un solde initial de zéro")
        void creationSoldeZero() {
            CompteBancaire compte = new CompteBancaire(2L, "Bob", BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, compte.getSolde());
        }

        @Test
        @DisplayName("Le solde initial ne peut pas être négatif")
        void soldeInitialNegatifInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00")));
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 2 : Dépôt (TDD itératif)
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Opérations de dépôt")
    class Depot {

        @Test
        @DisplayName("Un dépôt augmente le solde")
        void depotAugmenteSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.deposer(new BigDecimal("150.00"), "Salaire");
            assertEquals(new BigDecimal("650.00"), compte.getSolde());
        }

        @Test
        @DisplayName("Dépôt de zéro est interdit")
        void depotZeroInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.deposer(BigDecimal.ZERO, "Dépôt nul"));
        }

        @Test
        @DisplayName("Dépôt négatif est interdit")
        void depotNegatifInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.deposer(new BigDecimal("-50.00"), "Dépôt négatif"));
        }

        @Test
        @DisplayName("Plusieurs dépôts successifs")
        void plusieursDepots() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Dépôt 1");
            compte.deposer(new BigDecimal("75.00"), "Dépôt 2");
            compte.deposer(new BigDecimal("25.00"), "Dépôt 3");
            assertEquals(new BigDecimal("250.00"), compte.getSolde());
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 3 : Retrait (TDD)
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Opérations de retrait")
    class Retrait {

        @Test
        @DisplayName("Un retrait diminue le solde")
        void retraitDiminueSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.retirer(new BigDecimal("200.00"), "Retrait DAB");
            assertEquals(new BigDecimal("300.00"), compte.getSolde());
        }

        @Test
        @DisplayName("Retrait de la totalité du solde")
        void retraitTotal() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.retirer(new BigDecimal("100.00"), "Retrait total");
            assertEquals(BigDecimal.ZERO, compte.getSolde());
        }

        @Test
        @DisplayName("Retrait supérieur au solde est interdit")
        void retraitSuperieurAuSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(new BigDecimal("200.00"), "Trop"));
        }

        @Test
        @DisplayName("Retrait de zéro interdit")
        void retraitZeroInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(BigDecimal.ZERO, "Zéro"));
        }

        @Test
        @DisplayName("Retrait négatif interdit")
        void retraitNegatifInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(new BigDecimal("-50.00"), "Négatif"));
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 4 : Historique des transactions (TDD)
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Historique des transactions")
    class Historique {

        @Test
        @DisplayName("Le compte neuf a un historique vide")
        void compteNeufHistoriqueVide() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertTrue(compte.getHistorique().isEmpty());
            assertEquals(0, compte.getNombreTransactions());
        }

        @Test
        @DisplayName("Un dépôt crée une transaction dans l'historique")
        void depotCreeTransaction() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Dépôt test");

            assertEquals(1, compte.getNombreTransactions());
            Transaction derniere = compte.getDerniereTransaction();
            assertNotNull(derniere);
            assertEquals(Transaction.Type.DEPOT, derniere.getType());
            assertEquals(new BigDecimal("50.00"), derniere.getMontant());
            assertEquals(new BigDecimal("150.00"), derniere.getSoldeApresOperation());
        }

        @Test
        @DisplayName("Un retrait crée une transaction dans l'historique")
        void retraitCreeTransaction() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.retirer(new BigDecimal("30.00"), "Retrait test");

            Transaction derniere = compte.getDerniereTransaction();
            assertEquals(Transaction.Type.RETRAIT, derniere.getType());
            assertEquals(new BigDecimal("30.00"), derniere.getMontant());
            assertEquals(new BigDecimal("70.00"), derniere.getSoldeApresOperation());
        }

        @Test
        @DisplayName("Plusieurs opérations créent des transactions ordonnées")
        void plusieursOperationsHistorique() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("0.00"));
            compte.deposer(new BigDecimal("1000.00"), "Salaire");
            compte.retirer(new BigDecimal("200.00"), "Loyer");
            compte.deposer(new BigDecimal("50.00"), "Remboursement");

            List<Transaction> historique = compte.getHistorique();
            assertEquals(3, historique.size());

            assertEquals(Transaction.Type.DEPOT, historique.get(0).getType());
            assertEquals(Transaction.Type.RETRAIT, historique.get(1).getType());
            assertEquals(Transaction.Type.DEPOT, historique.get(2).getType());

            assertEquals(new BigDecimal("1000.00"), historique.get(0).getSoldeApresOperation());
            assertEquals(new BigDecimal("800.00"), historique.get(1).getSoldeApresOperation());
            assertEquals(new BigDecimal("850.00"), historique.get(2).getSoldeApresOperation());
        }

        @Test
        @DisplayName("L'historique est immuable (ne peut pas être modifié de l'extérieur)")
        void historiqueImmuable() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Test");

            List<Transaction> historique = compte.getHistorique();
            assertThrows(UnsupportedOperationException.class,
                () -> historique.add(null),
                "La liste retournée doit être non modifiable");
        }

        @Test
        @DisplayName("getDerniereTransaction retourne null si historique vide")
        void derniereTransactionNullSiVide() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertNull(compte.getDerniereTransaction());
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 5 : Virements internes (TDD)
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Virements internes au compte")
    class OperationsVirement {

        @Test
        @DisplayName("Émission d'un virement débite le compte")
        void emissionVirementDebite() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
            compte.emettreVirement(new BigDecimal("300.00"), "Paiement facture");
            assertEquals(new BigDecimal("700.00"), compte.getSolde());
        }

        @Test
        @DisplayName("Réception d'un virement crédite le compte")
        void receptionVirementCredite() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.recevoirVirement(new BigDecimal("200.00"), "Remboursement");
            assertEquals(new BigDecimal("700.00"), compte.getSolde());
        }

        @Test
        @DisplayName("Le virement émis crée une transaction de type VIREMENT_EMIS")
        void transactionVirementEmis() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.emettreVirement(new BigDecimal("100.00"), "Test virement");
            assertEquals(Transaction.Type.VIREMENT_EMIS,
                compte.getDerniereTransaction().getType());
        }

        @Test
        @DisplayName("Le virement reçu crée une transaction de type VIREMENT_RECU")
        void transactionVirementRecu() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.recevoirVirement(new BigDecimal("100.00"), "Test réception");
            assertEquals(Transaction.Type.VIREMENT_RECU,
                compte.getDerniereTransaction().getType());
        }

        @Test
        @DisplayName("Virement émis supérieur au solde interdit")
        void virementEmisSuperieurSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.emettreVirement(new BigDecimal("200.00"), "Trop"));
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 6 : Tests de thread-safety
     * ────────────────────────────────────────────────────────────────────
     */

    @Nested
    @DisplayName("Concurrence et thread-safety")
    class Concurrence {

        @Test
        @DisplayName("Dépôts concurrents : le solde final est correct")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void depotsConcurrents() throws InterruptedException {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", BigDecimal.ZERO);
            int nbThreads = 10;
            int nbOperations = 100;
            BigDecimal montant = BigDecimal.ONE;

            ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
            CountDownLatch latch = new CountDownLatch(nbThreads);

            for (int i = 0; i < nbThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < nbOperations; j++) {
                            compte.deposer(montant, "Dépôt concurrent");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            BigDecimal attendu = new BigDecimal(nbThreads * nbOperations);
            assertEquals(attendu, compte.getSolde(),
                "Avec " + nbThreads + " threads × " + nbOperations + " dépôts de 1€, le solde doit être " + attendu + "€");
            assertEquals(nbThreads * nbOperations, compte.getNombreTransactions());
        }

        @Test
        @DisplayName("Dépôts et retraits concurrents : intégrité du solde")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void operationsMixtesConcurrentes() throws InterruptedException {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
            int nbThreads = 5;
            BigDecimal montant = new BigDecimal("1.00");

            ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
            CountDownLatch latch = new CountDownLatch(nbThreads);

            for (int i = 0; i < nbThreads / 2 + 1; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; j++) {
                            compte.deposer(montant, "Dépôt");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            for (int i = nbThreads / 2 + 1; i < nbThreads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; j++) {
                            compte.retirer(montant, "Retrait");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            int nbDepots = 3 * 50;  // 3 threads dépot à 50 ops
            int nbRetraits = 2 * 50; // 2 threads retrait à 50 ops
            BigDecimal attendu = new BigDecimal("1000.00")
                .add(new BigDecimal(nbDepots))
                .subtract(new BigDecimal(nbRetraits));

            assertEquals(attendu, compte.getSolde());
        }
    }

    /*
     * ────────────────────────────────────────────────────────────────────
     * ÉTAPE 7 : @RepeatedTest — tests répétés
     * ────────────────────────────────────────────────────────────────────
     */

    @RepeatedTest(value = 10, name = "{displayName} — répétition {currentRepetition}/{totalRepetitions}")
    @DisplayName("Stabilité : le solde après 3 opérations est toujours correct")
    void stabiliteSoldeApresOperations() {
        CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00"));
        compte.deposer(new BigDecimal("50.00"), "Depot");
        compte.retirer(new BigDecimal("30.00"), "Retrait");
        compte.deposer(new BigDecimal("20.00"), "Depot 2");
        assertEquals(new BigDecimal("140.00"), compte.getSolde(),
            "Après 100+50-30+20 le solde doit toujours être 140.00");
    }
}
