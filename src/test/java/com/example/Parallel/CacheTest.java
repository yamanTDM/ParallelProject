package com.example.Parallel;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.Product;
import com.example.Parallel.repository.ProductRepository;
import com.example.Parallel.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class CacheTest {

    private final ProductService productService;
    private final ProductRepository productRepository;

    @Autowired
    CacheTest(ProductService productService,
              ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @Test
    public void compareWithCacheAndWithoutCache() {


        int iterations = 100;

        long noCacheStart = System.currentTimeMillis();
        productService.getAllWithoutCache();

        for (int i = 0; i < iterations; i++) {
            productService.getAllWithoutCache();
        }

        long noCacheEnd = System.currentTimeMillis();

        productService.getAll();

        long cacheStart = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            List<ProductDto.Response> products = productService.getAll();
        }

        long cacheEnd = System.currentTimeMillis();

        System.out.println("\n\n========================================");
        System.out.println("CACHE TEST (" + iterations + " iterations)");
        System.out.println("========================================");
        System.out.println("Without Cache : " + (noCacheEnd - noCacheStart) + " ms");
        System.out.println("With Cache    : " + (cacheEnd - cacheStart) + " ms");
        System.out.println("========================================\n\n");

    }
}