package com.example.Parallel.controller;

import com.example.Parallel.dto.OrderDto;
import com.example.Parallel.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @PostMapping
    public ResponseEntity<OrderDto.Response> createOrder(
            @Valid @RequestBody OrderDto.Request request,
            Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(email, request));
    }


    @GetMapping
    public ResponseEntity<List<OrderDto.Response>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getName()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDto.Response> cancelOrder(
            @PathVariable Integer id,
            Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(id, auth.getName()));
    }
}
