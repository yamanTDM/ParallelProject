package com.example.Parallel;

import com.example.Parallel.dto.CartDto;
import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.Product;
import com.example.Parallel.entity.User;
import com.example.Parallel.repository.*;
import com.example.Parallel.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;


@SpringBootTest
class RaceConditionTest {
    private final ProductService productService;
    private final ProductRepository productRepository;

    @Autowired
    RaceConditionTest(ProductService productService,
                      ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    Integer stock = 100;

    @Test
    public void simulateRace() throws InterruptedException {
        int stockAfterSafe;
        int stockAfterUnsafe;
        long start = System.currentTimeMillis();
        ProductDto.Request requestSafe = new ProductDto.Request();
        requestSafe.setName("SAFE - Race Condition Test Product ");
        requestSafe.setDescription("Test Description");
        requestSafe.setPrice(BigDecimal.valueOf(1.0));
        requestSafe.setStock(stock);
        ProductDto.Response responseSafe = productService.create(requestSafe);
        ProductDto.Request requestUnSafe = new ProductDto.Request();
        requestUnSafe.setName("NOT SAFE - Race Condition Test Product ");
        requestUnSafe.setDescription("Test Description");
        requestUnSafe.setPrice(BigDecimal.valueOf(1.0));
        requestUnSafe.setStock(stock);
        ProductDto.Response responseUnSafe = productService.create(requestUnSafe);
        int productSafeId = responseSafe.getId();
        int productUnsafeId = responseUnSafe.getId();

        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (int i = 0; i < stock; i++) {
            executor.submit(() -> {
                productService.purchaseSafeTest(productSafeId);
            });
        }
        for (int i = 0; i < stock; i++) {
            executor.submit(() -> {
                productService.purchaseUnsafeTest(productUnsafeId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long end = System.currentTimeMillis();
        Product productSafe = productService.findOrThrow(productSafeId);
        Product productUnSafe = productService.findOrThrowUnSafe(productUnsafeId);
        stockAfterSafe = productSafe.getStock();
        stockAfterUnsafe = productUnSafe.getStock();
        productRepository.deleteById(productSafeId);
        productRepository.deleteById(productUnsafeId);
        System.out.println("\n========================================");
        System.out.println("Race Condition TEST");
        System.out.println("========================================\n");
        System.out.println("Test time:" + (end - start) + "ms");
        System.out.println("Safe stock: BEFORE " + stock + " | AFTER " + stockAfterSafe);
        System.out.println("Not Safe stock: BEFORE " + stock + " | AFTER " + stockAfterUnsafe);
        System.out.println("\n");

    }
}