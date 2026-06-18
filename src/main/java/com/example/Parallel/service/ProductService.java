package com.example.Parallel.service;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.Product;
import com.example.Parallel.exception.BadRequestException;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AsyncTaskService asyncTaskService;


    //to get all products
    @Cacheable(value = "products", key = "'all'")
    public List<ProductDto.Response> getAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    //to get all products without caching TESTING
    public List<ProductDto.Response> getAllWithoutCache() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    //to get a specific product
    @Cacheable(value = "product", key = "#id")
    @Transactional
    public ProductDto.Response getById(Integer id) {
        Product product = findOrThrowUnSafe(id);
        return toResponse(product);
    }

    //to create a product
    @CacheEvict(value = "products", key = "'all'", beforeInvocation = true)
    public ProductDto.Response create(ProductDto.Request request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return toResponse(productRepository.save(product));
    }

    //to update a product
    @CachePut(value = "product", key = "#id")
    @CacheEvict(value = "products", key = "'all'", beforeInvocation = true)
    @Transactional
    public ProductDto.Response update(Integer id, ProductDto.Request request) {
        Product product = findOrThrow(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return toResponse(productRepository.save(product));
    }

    //to update product stock
    //Synchronization Point
    @CachePut(value = "product", key = "#productId")
    @CacheEvict(value = "products", key = "'all'", beforeInvocation = true)
    @Transactional
    public ProductDto.Response updateStock(Integer productId, int quantity) {
        Product product = productRepository.findProductAndLock(productId); // read UNDER lock
        if (product.getStock() < quantity) {
            throw new BadRequestException("Not enough stock for product " + productId);
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return toResponse(product);
    }

    //to delete a product
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "products", key = "'all'")
    })
    public void delete(Integer id) {
        findOrThrow(id);
        productRepository.deleteById(id);
    }

    //to purchase a product while waiting for background tasks to finish TESTING
    //Synchronization Point
    @CacheEvict(value = "products", key = "'all'")
    @CachePut(value = "product", key = "#productId")
    @Transactional
    public ProductDto.Response purchaseNoAsyncTest(Integer productId) {
        Product product = productRepository.findProductAndLock(productId);
        product.setStock(product.getStock() - 1);
        asyncTaskService.sendOrderConfirmationEmailNoAsync("email", 1);
        asyncTaskService.updateStatisticsNoAsync(1);
        return toResponse(productRepository.save(product));

    }


    //to purchase a product without waiting for background tasks to finish TESTING
    //Synchronization Point
    @CacheEvict(value = "products", key = "'all'")
    @CachePut(value = "product", key = "#productId")
    @Transactional
    public ProductDto.Response purchaseSafeTest(Integer productId) {
        Product product = productRepository.findProductAndLock(productId);
        product.setStock(product.getStock() - 1);
        asyncTaskService.sendOrderConfirmationEmail("email", 1);
        asyncTaskService.updateStatistics(1);
        return toResponse(productRepository.save(product));

    }

    //to purchase a product with applying a lock TESTING
    //Synchronization Point
    @CacheEvict(value = "products", key = "'all'")
    @CachePut(value = "product", key = "#productId")
    @Transactional
    public ProductDto.Response purchaseSafeLockTest(Integer productId) {
        Product product = productRepository.findProductAndLock(productId);
        product.setStock(product.getStock() - 1);
        return toResponse(productRepository.save(product));

    }

    //to purchase a product without applying a lock TESTING
    @CacheEvict(value = "products", key = "'all'")
    @CachePut(value = "product", key = "#productId")
    @Transactional
    public ProductDto.Response purchaseUnsafeTest(Integer productId) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setStock(product.getStock() - 1);
        return toResponse(productRepository.save(product));

    }

    //to get a product and lock it
    //Synchronization Point
    @Transactional
    public Product findOrThrow(Integer id) {
        try {
            return productRepository.findProductAndLock(id);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Product with id " + id + " not found");
        }
    }

    //to get a product without locking it
    @Transactional
    public Product findOrThrowUnSafe(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    }

    //to purchase a product and fail afterward with rollback TESTING
    @Transactional
    public void purchaseWithTransactionThenFail(int productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));
        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        throw new RuntimeException("Payment Failed");
    }

    //to purchase a product and fail afterward without rollback TESTING
    public void purchaseWithoutTransactionThenFail(int productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));
        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        throw new RuntimeException("Payment Failed");
    }

    private ProductDto.Response toResponse(Product p) {
        ProductDto.Response res = new ProductDto.Response();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setDescription(p.getDescription());
        res.setPrice(p.getPrice());
        res.setStock(p.getStock());
        return res;
    }
}