package com.example.Parallel.controller;

import com.example.Parallel.dto.PaymentDto;
import com.example.Parallel.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto.Response> pay(
            @Valid @RequestBody PaymentDto.Request request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(request, auth.getName()));
    }
}
