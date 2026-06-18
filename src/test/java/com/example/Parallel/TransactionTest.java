package com.example.Parallel;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.repository.ProductRepository;
import com.example.Parallel.service.ProductService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;

    private int productTransactionalId;
    private int productNoTransactionId;
    private static final int INITIAL_STOCK = 5;

    private int    safeStockAfter;
    private int    unsafeStockAfter;

    @BeforeAll
    void setup() {
        ProductDto.Request req1 = new ProductDto.Request();
        req1.setName("WITH Transaction");
        req1.setDescription("Stock should NOT change if purchase fails");
        req1.setPrice(BigDecimal.valueOf(9.99));
        req1.setStock(INITIAL_STOCK);
        productTransactionalId = productService.create(req1).getId();

        ProductDto.Request req2 = new ProductDto.Request();
        req2.setName("WITHOUT Transaction");
        req2.setDescription("Stock WILL change even though purchase fails");
        req2.setPrice(BigDecimal.valueOf(9.99));
        req2.setStock(INITIAL_STOCK);
        productNoTransactionId = productService.create(req2).getId();
    }

    @AfterAll
    void printResultsAndCleanup() {
        System.out.println("\n\n========================================================");
        System.out.println("            TRANSACTION ROLLBACK TEST RESULTS           ");
        System.out.println("========================================================");
        System.out.printf("  Initial stock          :  %d%n", INITIAL_STOCK);
        System.out.println("--------------------------------------------------------");
        System.out.printf("  WITH transaction    →  stock after: %d%n", safeStockAfter);
        System.out.printf("  WITHOUT transaction →  stock after: %d%n", unsafeStockAfter);
        System.out.println("--------------------------------------------------------");

        productRepository.deleteById(productTransactionalId);
        productRepository.deleteById(productNoTransactionId);
    }

    @Test
    void withTransaction_stockShouldRollbackOnError() {
        try {
            productService.purchaseWithTransactionThenFail(productTransactionalId);
        } catch (RuntimeException e) {
        }
        safeStockAfter = productService.findOrThrow(productTransactionalId).getStock();
    }

    @Test
    void withoutTransaction_stockShouldBeCorruptedOnError() {
        try {
            productService.purchaseWithoutTransactionThenFail(productNoTransactionId);
        } catch (RuntimeException e) {
        }
        unsafeStockAfter = productService.findOrThrow(productNoTransactionId).getStock();
    }
}