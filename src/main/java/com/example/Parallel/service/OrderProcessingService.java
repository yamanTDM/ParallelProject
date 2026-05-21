package com.example.Parallel.service;

import com.example.Parallel.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrderProcessingService {
    private final CartService cartService;

    @Async("orderExecutor")
    public CompletableFuture<OrderDto.Response> checkoutAsync(String email) {
        try {
            OrderDto.Response result = cartService.checkout(email);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
