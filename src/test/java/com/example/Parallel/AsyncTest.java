package com.example.Parallel;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.repository.ProductRepository;
import com.example.Parallel.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class AsyncTest {

    private final ProductService productService;
    private final ProductRepository productRepository;

    @Autowired
    AsyncTest(ProductService productService,
              ProductRepository productRepository) {

        this.productService = productService;
        this.productRepository = productRepository;
    }

    Integer stock = 3;

    @Test
    public void simulateAsync() throws InterruptedException {


        ProductDto.Request requestSafe = new ProductDto.Request();
        requestSafe.setName("ASYNC Purchase");
        requestSafe.setDescription("Test Description");
        requestSafe.setPrice(BigDecimal.valueOf(1.0));
        requestSafe.setStock(stock);

        ProductDto.Response responseSafe =
                productService.create(requestSafe);

        int productSafeId = responseSafe.getId();

        ExecutorService safeExecutor =
                Executors.newFixedThreadPool(5);

        long safeStart = System.currentTimeMillis();

        for (int i = 0; i < stock; i++) {

            safeExecutor.submit(() -> {
                productService.purchaseSafeTest(productSafeId);
            });
        }

        safeExecutor.shutdown();

        safeExecutor.awaitTermination(30, TimeUnit.SECONDS);

        long safeEnd = System.currentTimeMillis();

        System.out.println("========================================");
        System.out.println("========================================");


        ProductDto.Request requestNoAsync = new ProductDto.Request();
        requestNoAsync.setName("NO ASYNC Purchase");
        requestNoAsync.setDescription("Test Description");
        requestNoAsync.setPrice(BigDecimal.valueOf(1.0));
        requestNoAsync.setStock(stock);

        ProductDto.Response responseNoAsync =
                productService.create(requestNoAsync);

        int productUnsafeId = responseNoAsync.getId();



        ExecutorService noAsyncExecutor =
                Executors.newFixedThreadPool(5);

        long noAsyncStart = System.currentTimeMillis();

        for (int i = 0; i < stock; i++) {

            noAsyncExecutor.submit(() -> {
                productService.purchaseNoAsyncTest(productUnsafeId);
            });
        }

        noAsyncExecutor.shutdown();

        noAsyncExecutor.awaitTermination(30, TimeUnit.SECONDS);

        long noAsyncEnd = System.currentTimeMillis();



        System.out.println("\n\n========================================");
        System.out.println("Async TEST");
        System.out.println("========================================");

        System.out.println("Purchase with async Time: "
                + (safeEnd - safeStart) + " ms");


        System.out.println("Purchase without async Time: "
                + (noAsyncEnd - noAsyncStart) + " ms\n\n");


        productRepository.deleteById(productSafeId);
        productRepository.deleteById(productUnsafeId);
    }
}