package com.example.Parallel.controller;

import com.example.Parallel.dto.CartDto;
import com.example.Parallel.dto.OrderDto;
import com.example.Parallel.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import com.example.Parallel.service.OrderProcessingService;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final OrderProcessingService orderProcessingService;

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto.Response> getMyCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCartResponse(auth.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto.Response> addItemToCart(
            @Valid @RequestBody CartDto.AddItemRequest request,
            Authentication auth) {
        return ResponseEntity.ok(cartService.addItemToCart(auth.getName(), request));
    }


    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto.Response> removeItem(
            @PathVariable Integer productId,
            Authentication auth) {
        return ResponseEntity.ok(cartService.removeItemFromCart(auth.getName(), productId));
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderDto.Response> checkoutCart(Authentication auth) {
        try {
            CompletableFuture<OrderDto.Response> future =
                    orderProcessingService.checkoutAsync(auth.getName());

            return ResponseEntity.ok(future.get());
        } catch (RejectedExecutionException e) {
            return ResponseEntity.status(503).build();
        } catch (Exception e) {
            throw new RuntimeException(e.getCause());
        }
    }

}