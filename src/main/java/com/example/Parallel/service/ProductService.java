package com.example.Parallel.service;

import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.Product;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AsyncTaskService asyncTaskService;

    public List<ProductDto.Response> getAll() {
        return productRepository.findAll()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }


    public ProductDto.Response getById(Integer id) {
        Product product = findOrThrow(id);
        return toResponse(product);
    }


    public ProductDto.Response create(ProductDto.Request request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return toResponse(productRepository.save(product));
    }


    public ProductDto.Response update(Integer id, ProductDto.Request request) {
        Product product = findOrThrow(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return toResponse(productRepository.save(product));
    }


    public void delete(Integer id) {
        findOrThrow(id);
        productRepository.deleteById(id);
    }

    @Transactional
    public Product findOrThrow(Integer id) {
        try {
            return productRepository.findProductAndLock(id);

        }catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Product with id " + id + " not found");
        }
    }
    @Transactional
    public Product findOrThrowUnSafe(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    }
    @Transactional
    public void purchaseNoAsyncTest(Integer productId) {

        Product product = productRepository.findProductAndLock(productId);
        product.setStock(product.getStock() - 1);

        asyncTaskService.sendOrderConfirmationEmailNoAsync("email",1);
        asyncTaskService.updateStatisticsNoAsync(1);
    }
    @Transactional
    public void purchaseSafeTest(Integer productId) {

        Product product = productRepository.findProductAndLock(productId);
        product.setStock(product.getStock() - 1);

        asyncTaskService.sendOrderConfirmationEmail("email",1);
        asyncTaskService.updateStatistics(1);
    }

    @Transactional
    public void purchaseUnsafeTest(Integer productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow();

        product.setStock(product.getStock() - 1);
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
