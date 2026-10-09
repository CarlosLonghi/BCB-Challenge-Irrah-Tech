package com.carloslonghi.bcb.integration;

import com.carloslonghi.bcb.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Prova o @Lock(PESSIMISTIC_WRITE) de findWithLockById: com mock isso nao e testavel,
// e sem a anotacao o metodo continua funcionando (o Spring Data ignora o "WithLock" do nome), so que sem travar
class ClientRepositoryLockIT extends IntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("findWithLockById trava a linha: uma segunda transação espera a primeira terminar")
    void secondTransactionWaitsForLock() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(tx -> {
                clientRepository.findWithLockById(clientId).orElseThrow();
                firstLocked.countDown();
                await(releaseFirst);
            }), executor);
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Void> second = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(tx ->
                    clientRepository.findWithLockById(clientId).orElseThrow()), executor);

            // Enquanto a primeira transacao segura o lock, a segunda fica bloqueada no SELECT ... FOR UPDATE
            Thread.sleep(500);
            assertThat(second).as("a segunda leitura deveria esperar o lock").isNotDone();

            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertThat(second).isCompleted();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("findById comum não trava: a leitura não espera a transação que segura o lock")
    void plainReadDoesNotWait() throws Exception {
        Long clientId = createPrePaidClient("12345678901", "10.00");
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<Void> first = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(tx -> {
                clientRepository.findWithLockById(clientId).orElseThrow();
                firstLocked.countDown();
                await(releaseFirst);
            }), executor);
            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();

            assertThat(clientRepository.findById(clientId)).isPresent();

            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
