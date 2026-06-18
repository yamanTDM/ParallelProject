package com.example.Parallel;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.Product;
import com.example.Parallel.repository.ProductRepository;
import com.example.Parallel.service.ProductService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DistributedRaceConditionTest {


    private static final int SERVER_COUNT   = 3;

    private static final int TOTAL_ATTEMPTS = 100;

    private static final int INITIAL_STOCK  = TOTAL_ATTEMPTS;


    private final List<ConfigurableApplicationContext> serverContexts = new ArrayList<>();
    private final List<ProductService>                 serverServices  = new ArrayList<>();

    private ProductRepository productRepository;
    private int               productSafeId;
    private int               productUnsafeId;


    @BeforeAll
    void bootServers() {
        System.out.println("\n[SETUP] Booting " + SERVER_COUNT + " application contexts...");

        for (int i = 0; i < SERVER_COUNT; i++) {
            ConfigurableApplicationContext ctx = SpringApplication.run(
                    ParallelApplication.class,
                    "--server.port=" + (9001 + i),
                    "--spring.jpa.hibernate.ddl-auto=none"
            );
            serverContexts.add(ctx);
            serverServices.add(ctx.getBean(ProductService.class));
        }

        productRepository = serverContexts.get(0).getBean(ProductRepository.class);

        ProductService s0 = serverServices.get(0);

        ProductDto.Request safeReq = new ProductDto.Request();
        safeReq.setName("SAFE - Distributed Lock Test");
        safeReq.setDescription("Tests distributed pessimistic lock");
        safeReq.setPrice(BigDecimal.valueOf(1.0));
        safeReq.setStock(INITIAL_STOCK);
        productSafeId = s0.create(safeReq).getId();

        ProductDto.Request unsafeReq = new ProductDto.Request();
        unsafeReq.setName("NOT SAFE - Distributed Lock Test");
        unsafeReq.setDescription("Tests distributed NO lock");
        unsafeReq.setPrice(BigDecimal.valueOf(1.0));
        unsafeReq.setStock(INITIAL_STOCK);
        productUnsafeId = s0.create(unsafeReq).getId();

        System.out.println("[SETUP] Products created — safeId=" + productSafeId
                + "  unsafeId=" + productUnsafeId);
    }

    @AfterAll
    void shutdown() {
        try {
            productRepository.deleteById(productSafeId);
            productRepository.deleteById(productUnsafeId);
        } catch (Exception ignored) {}

        serverContexts.forEach(ConfigurableApplicationContext::close);
        System.out.println("[TEARDOWN] All server contexts closed.");
    }


    @Test
    void distributedLockShouldPreventOverselling() throws InterruptedException {


        ExecutorService executor = Executors.newFixedThreadPool(TOTAL_ATTEMPTS/2);
        CountDownLatch   latch   = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < TOTAL_ATTEMPTS; i++) {
            final int serverIndex = i % SERVER_COUNT;
            final ProductService svc = serverServices.get(serverIndex);

            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    svc.purchaseSafeLockTest(productSafeId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        for (int i = 0; i < TOTAL_ATTEMPTS; i++) {
            final int serverIndex = i % SERVER_COUNT;
            final ProductService svc = serverServices.get(serverIndex);

            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    svc.purchaseUnsafeTest(productUnsafeId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        long start = System.currentTimeMillis();
        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        Product safeProduct   = serverServices.get(0).findOrThrow(productSafeId);
        Product unsafeProduct = serverServices.get(0).findOrThrowUnSafe(productUnsafeId);

        int safeStock   = safeProduct.getStock();
        int unsafeStock = unsafeProduct.getStock();

        System.out.println("\n========================================================");
        System.out.println("       DISTRIBUTED RACE CONDITION TEST RESULTS          ");
        System.out.println("========================================================");
        System.out.printf("  Threads per test  :  %d (%.0f/server)%n", TOTAL_ATTEMPTS,
                (double) TOTAL_ATTEMPTS / SERVER_COUNT);
        System.out.printf("  Elapsed time      :  %d ms%n",            elapsed);
        System.out.println("--------------------------------------------------------");
        System.out.printf("  SAFE   stock  →  before: %3d  |  after: %3d%n",
                INITIAL_STOCK, safeStock);
        System.out.printf("  UNSAFE stock  →  before: %3d  |  after: %3d%n",
                INITIAL_STOCK, unsafeStock);


    }
}
