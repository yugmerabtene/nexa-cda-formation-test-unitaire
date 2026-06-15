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
 * Tests unitaires pour la classe {@link CompteBancaire}.
 * <p>
 * Cette classe de test suit une organisation par groupes fonctionnels via
 * l'annotation {@link Nested}, facilitant la lisibilite et le diagnostic
 * des echecs. Chaque groupe est affiche avec un nom explicite via
 * {@link DisplayName}.
 * </p>
 *
 * <p>Groupes de tests :</p>
 * <ul>
 *   <li>Creation du compte</li>
 *   <li>Operations de depot</li>
 *   <li>Operations de retrait</li>
 *   <li>Historique des transactions</li>
 *   <li>Virements internes au compte</li>
 *   <li>Concurrence et thread-safety</li>
 *   <li>Stabilite (test repete)</li>
 * </ul>
 */
@DisplayName("TDD : Compte Bancaire")
class CompteBancaireTest {

    /**
     * Tests de creation d'un compte bancaire.
     * <p>Verifie que le constructeur initialise correctement les attributs
     * et rejette les valeurs invalides (solde negatif).</p>
     */
    @Nested
    @DisplayName("Creation du compte")
    class CreationCompte {

        /**
         * Verifie qu'un compte est cree avec l'ID, le titulaire et le solde initial fournis.
         * <p>Cas nominal : tous les parametres sont valides.</p>
         */
        @Test
        @DisplayName("Un compte est cree avec un solde initial correct")
        void creationAvecSoldeInitial() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));

            assertEquals(1L, compte.getId());
            assertEquals("Alice", compte.getTitulaire());
            assertEquals(new BigDecimal("1000.00"), compte.getSolde());
        }

        /**
         * Verifie qu'un compte peut etre cree avec un solde initial de zero.
         * <p>Cas limite : le solde zero est accepte (compte sans provision).</p>
         */
        @Test
        @DisplayName("Un compte peut etre cree avec un solde initial de zero")
        void creationSoldeZero() {
            CompteBancaire compte = new CompteBancaire(2L, "Bob", BigDecimal.ZERO);
            assertEquals(BigDecimal.ZERO, compte.getSolde());
        }

        /**
         * Verifie que la creation avec un solde initial negatif leve une exception.
         * <p>Cas d'erreur : le constructeur doit rejeter un solde negatif.</p>
         */
        @Test
        @DisplayName("Le solde initial ne peut pas etre negatif")
        void soldeInitialNegatifInterdit() {
            assertThrows(IllegalArgumentException.class,
                () -> new CompteBancaire(3L, "Charlie", new BigDecimal("-100.00")));
        }
    }

    /**
     * Tests des operations de depot sur un compte bancaire.
     * <p>Verifie l'incrementation correcte du solde et le rejet des depots
     * nuls ou negatifs.</p>
     */
    @Nested
    @DisplayName("Operations de depot")
    class Depot {

        /**
         * Verifie qu'un depot augmente le solde du montant exact depose.
         * <p>Cas nominal : depot positif sur un compte provisionne.</p>
         */
        @Test
        @DisplayName("Un depot augmente le solde")
        void depotAugmenteSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.deposer(new BigDecimal("150.00"), "Salaire");
            assertEquals(new BigDecimal("650.00"), compte.getSolde());
        }

        /**
         * Verifie qu'un depot de zero est interdit.
         * <p>Cas d'erreur : le montant doit etre strictement positif.</p>
         */
        @Test
        @DisplayName("Depot de zero est interdit")
        void depotZeroInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.deposer(BigDecimal.ZERO, "Depot nul"));
        }

        /**
         * Verifie qu'un depot negatif est interdit.
         * <p>Cas d'erreur : un depot ne doit pas pouvoir diminuer le solde.</p>
         */
        @Test
        @DisplayName("Depot negatif est interdit")
        void depotNegatifInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.deposer(new BigDecimal("-50.00"), "Depot negatif"));
        }

        /**
         * Verifie que plusieurs depots successifs sont correctement cumules.
         * <p>Cas nominal : trois depots consecutifs sur un compte.</p>
         */
        @Test
        @DisplayName("Plusieurs depots successifs")
        void plusieursDepots() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Depot 1");
            compte.deposer(new BigDecimal("75.00"), "Depot 2");
            compte.deposer(new BigDecimal("25.00"), "Depot 3");
            assertEquals(new BigDecimal("250.00"), compte.getSolde());
        }
    }

    /**
     * Tests des operations de retrait sur un compte bancaire.
     * <p>Verifie la decrementation correcte du solde, le rejet des retraits
     * nuls/negatifs, et le blocage des retraits superieurs au solde.</p>
     */
    @Nested
    @DisplayName("Operations de retrait")
    class Retrait {

        /**
         * Verifie qu'un retrait diminue le solde du montant exact retire.
         * <p>Cas nominal : retrait partiel sur un compte provisionne.</p>
         */
        @Test
        @DisplayName("Un retrait diminue le solde")
        void retraitDiminueSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.retirer(new BigDecimal("200.00"), "Retrait DAB");
            assertEquals(new BigDecimal("300.00"), compte.getSolde());
        }

        /**
         * Verifie qu'on peut retirer la totalite du solde (solde final = 0).
         * <p>Cas limite : retrait egal au solde disponible.</p>
         */
        @Test
        @DisplayName("Retrait de la totalite du solde")
        void retraitTotal() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.retirer(new BigDecimal("100.00"), "Retrait total");
            assertEquals(BigDecimal.ZERO, compte.getSolde());
        }

        /**
         * Verifie qu'un retrait superieur au solde est interdit.
         * <p>Cas d'erreur : le compte ne doit jamais devenir negatif.</p>
         */
        @Test
        @DisplayName("Retrait superieur au solde est interdit")
        void retraitSuperieurAuSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(new BigDecimal("200.00"), "Trop"));
        }

        /**
         * Verifie qu'un retrait de zero est interdit.
         * <p>Cas d'erreur : le montant doit etre strictement positif.</p>
         */
        @Test
        @DisplayName("Retrait de zero interdit")
        void retraitZeroInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(BigDecimal.ZERO, "Zero"));
        }

        /**
         * Verifie qu'un retrait negatif est interdit.
         * <p>Cas d'erreur : un retrait negatif serait un depot deguise.</p>
         */
        @Test
        @DisplayName("Retrait negatif interdit")
        void retraitNegatifInterdit() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.retirer(new BigDecimal("-50.00"), "Negatif"));
        }
    }

    /**
     * Tests de l'historique des transactions.
     * <p>Verifie que chaque operation est correctement enregistree, que
     * l'historique respecte l'ordre chronologique, et que la liste exposee
     * est immuable.</p>
     */
    @Nested
    @DisplayName("Historique des transactions")
    class Historique {

        /**
         * Verifie que l'historique est vide a la creation du compte.
         * <p>Cas nominal : aucun appel a une methode d'operation, l'historique
         * est une liste vide et le nombre de transactions est 0.</p>
         */
        @Test
        @DisplayName("Le compte neuf a un historique vide")
        void compteNeufHistoriqueVide() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertTrue(compte.getHistorique().isEmpty());
            assertEquals(0, compte.getNombreTransactions());
        }

        /**
         * Verifie qu'un depot cree une transaction de type DEPOT avec les bons attributs.
         * <p>Controle : type = DEPOT, montant = montant depose,
         * soldeApresOperation = solde initial + depot.</p>
         */
        @Test
        @DisplayName("Un depot cree une transaction dans l'historique")
        void depotCreeTransaction() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Depot test");

            assertEquals(1, compte.getNombreTransactions());
            Transaction derniere = compte.getDerniereTransaction();
            assertNotNull(derniere);
            assertEquals(Transaction.Type.DEPOT, derniere.getType());
            assertEquals(new BigDecimal("50.00"), derniere.getMontant());
            assertEquals(new BigDecimal("150.00"), derniere.getSoldeApresOperation());
        }

        /**
         * Verifie qu'un retrait cree une transaction de type RETRAIT avec les bons attributs.
         * <p>Controle : type = RETRAIT, montant = montant retire,
         * soldeApresOperation = solde initial - retrait.</p>
         */
        @Test
        @DisplayName("Un retrait cree une transaction dans l'historique")
        void retraitCreeTransaction() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.retirer(new BigDecimal("30.00"), "Retrait test");

            Transaction derniere = compte.getDerniereTransaction();
            assertEquals(Transaction.Type.RETRAIT, derniere.getType());
            assertEquals(new BigDecimal("30.00"), derniere.getMontant());
            assertEquals(new BigDecimal("70.00"), derniere.getSoldeApresOperation());
        }

        /**
         * Verifie que plusieurs operations sont enregistrees dans l'ordre d'execution.
         * <p>Test de sequence : Depot(1000) -> Retrait(200) -> Depot(50).
         * Les soldes apres operation sont verifies pour chaque etape.</p>
         */
        @Test
        @DisplayName("Plusieurs operations creent des transactions ordonnees")
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

        /**
         * Verifie que la liste retournee par getHistorique() est non modifiable.
         * <p>Toute tentative d'ajout/retrait/modification doit lever
         * {@link UnsupportedOperationException}.</p>
         */
        @Test
        @DisplayName("L'historique est immuable (ne peut pas etre modifie de l'exterieur)")
        void historiqueImmuable() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            compte.deposer(new BigDecimal("50.00"), "Test");

            List<Transaction> historique = compte.getHistorique();
            assertThrows(UnsupportedOperationException.class,
                () -> historique.add(null),
                "La liste retournee doit etre non modifiable");
        }

        /**
         * Verifie que getDerniereTransaction() retourne null quand l'historique est vide.
         * <p>Cas limite : aucun appel a une methode d'operation.</p>
         */
        @Test
        @DisplayName("getDerniereTransaction retourne null si historique vide")
        void derniereTransactionNullSiVide() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertNull(compte.getDerniereTransaction());
        }
    }

    /**
     * Tests des operations de virement internes au compte (emission et reception).
     * <p>Ces methodes sont utilisees par {@code ServiceVirement} mais sont testees
     * independamment pour verifier leur comportement unitaire.</p>
     */
    @Nested
    @DisplayName("Virements internes au compte")
    class OperationsVirement {

        /**
         * Verifie que l'emission d'un virement debite le compte du montant indique.
         * <p>Cas nominal : compte avec solde suffisant.</p>
         */
        @Test
        @DisplayName("Emission d'un virement debite le compte")
        void emissionVirementDebite() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
            compte.emettreVirement(new BigDecimal("300.00"), "Paiement facture");
            assertEquals(new BigDecimal("700.00"), compte.getSolde());
        }

        /**
         * Verifie que la reception d'un virement credite le compte du montant indique.
         * <p>Cas nominal : la reception augmente le solde sans validation particuliere.</p>
         */
        @Test
        @DisplayName("Reception d'un virement credite le compte")
        void receptionVirementCredite() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.recevoirVirement(new BigDecimal("200.00"), "Remboursement");
            assertEquals(new BigDecimal("700.00"), compte.getSolde());
        }

        /**
         * Verifie que l'emission d'un virement enregistre une transaction de type VIREMENT_EMIS.
         * <p>Controle du type de transaction genere.</p>
         */
        @Test
        @DisplayName("Le virement emis cree une transaction de type VIREMENT_EMIS")
        void transactionVirementEmis() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.emettreVirement(new BigDecimal("100.00"), "Test virement");
            assertEquals(Transaction.Type.VIREMENT_EMIS,
                compte.getDerniereTransaction().getType());
        }

        /**
         * Verifie que la reception d'un virement enregistre une transaction de type VIREMENT_RECU.
         * <p>Controle du type de transaction genere.</p>
         */
        @Test
        @DisplayName("Le virement recu cree une transaction de type VIREMENT_RECU")
        void transactionVirementRecu() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("500.00"));
            compte.recevoirVirement(new BigDecimal("100.00"), "Test reception");
            assertEquals(Transaction.Type.VIREMENT_RECU,
                compte.getDerniereTransaction().getType());
        }

        /**
         * Verifie que l'emission d'un virement superieur au solde est interdite.
         * <p>Cas d'erreur : pas de decouvert autorise.</p>
         */
        @Test
        @DisplayName("Virement emis superieur au solde interdit")
        void virementEmisSuperieurSolde() {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("100.00"));
            assertThrows(IllegalArgumentException.class,
                () -> compte.emettreVirement(new BigDecimal("200.00"), "Trop"));
        }
    }

    /**
     * Tests de concurrence verifiant la thread-safety du compte bancaire.
     * <p>
     * Utilise un {@link ExecutorService} avec un pool de threads et un
     * {@link CountDownLatch} pour synchroniser les operations concurrentes.
     * Chaque test est limite a 5 secondes via {@link Timeout}.
     * </p>
     */
    @Nested
    @DisplayName("Concurrence et thread-safety")
    class Concurrence {

        /**
         * Verifie que des depots concurrents sur un compte produisent un solde final correct.
         * <p>
         * 10 threads effectuent chacun 100 depots de 1€. Le solde final attendu
         * est de 1000€ et le nombre de transactions doit etre egal a 1000.
         * Ce test garantit que la synchronisation sur {@code deposer()} empeche
         * les interferences entre threads.
         * </p>
         *
         * @throws InterruptedException si l'attente des threads est interrompue
         */
        @Test
        @DisplayName("Depots concurrents : le solde final est correct")
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
                            compte.deposer(montant, "Depot concurrent");
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
                "Avec " + nbThreads + " threads x " + nbOperations + " depots de 1€, le solde doit etre " + attendu + "€");
            assertEquals(nbThreads * nbOperations, compte.getNombreTransactions());
        }

        /**
         * Verifie l'integrite du solde lors d'operations mixtes concurrentes (depots et retraits).
         * <p>
         * 5 threads sont repartis : 3 effectuent 50 depots de 1€ chacun,
         * 2 effectuent 50 retraits de 1€ chacun. Le solde initial est de 1000€.
         * Le solde final attendu est de 1000 + 150 - 100 = 1050€.
         * Ce test verifie que les sections critiques de {@code deposer()} et
         * {@code retirer()} ne s'interferent pas.
         * </p>
         *
         * @throws InterruptedException si l'attente des threads est interrompue
         */
        @Test
        @DisplayName("Depots et retraits concurrents : integrite du solde")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void operationsMixtesConcurrentes() throws InterruptedException {
            CompteBancaire compte = new CompteBancaire(1L, "Alice", new BigDecimal("1000.00"));
            int nbThreads = 5;
            BigDecimal montant = new BigDecimal("1.00");

            ExecutorService executor = Executors.newFixedThreadPool(nbThreads);
            CountDownLatch latch = new CountDownLatch(nbThreads);

            // 3 threads de depot (indices 0, 1, 2)
            for (int i = 0; i < nbThreads / 2 + 1; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; j++) {
                            compte.deposer(montant, "Depot");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            // 2 threads de retrait (indices 3, 4)
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

            // Nombre de depots : 3 threads * 50 operations
            int nbDepots = 3 * 50;  
            // Nombre de retraits : 2 threads * 50 operations
            int nbRetraits = 2 * 50; 

            BigDecimal attendu = new BigDecimal("1000.00")
                .add(new BigDecimal(nbDepots))
                .subtract(new BigDecimal(nbRetraits));

            assertEquals(attendu, compte.getSolde());
        }
    }

    /**
     * Test de stabilite repete 10 fois pour verifier le determinisme des operations.
     * <p>
     * La meme sequence d'operations (depot 50, retrait 30, depot 20) est executee
     * sur un compte partant de 100€. Le resultat attendu est toujours 140€,
     * quelle que soit l'iteration. L'annotation {@link RepeatedTest} garantit
     * que le test est execute 10 fois de suite.
     * </p>
     * <p>
     * Ce test permet de detecter d'eventuels problemes de synchronisation subtils
     * qui pourraient apparaitre de maniere non deterministe.
     * </p>
     */
    @RepeatedTest(value = 10, name = "{displayName} — repetition {currentRepetition}/{totalRepetitions}")
    @DisplayName("Stabilite : le solde apres 3 operations est toujours correct")
    void stabiliteSoldeApresOperations() {
        CompteBancaire compte = new CompteBancaire(1L, "Test", new BigDecimal("100.00"));
        compte.deposer(new BigDecimal("50.00"), "Depot");
        compte.retirer(new BigDecimal("30.00"), "Retrait");
        compte.deposer(new BigDecimal("20.00"), "Depot 2");
        assertEquals(new BigDecimal("140.00"), compte.getSolde(),
            "Apres 100+50-30+20 le solde doit toujours etre 140.00");
    }
}
