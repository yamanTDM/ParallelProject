package com.example.Parallel.service;

import com.example.Parallel.dto.PaymentDto;
import com.example.Parallel.entity.*;
import com.example.Parallel.exception.BadRequestException;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.OrderRepository;
import com.example.Parallel.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;


    @Transactional
    public PaymentDto.Response processPayment(PaymentDto.Request request, String email) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "order " + request.getOrderId() + " not found"));

        if (!order.getUser().getEmail().equals(email)) {
            throw new BadRequestException("Can't process payment with different email");
        }

        if (order.getStatus() != Order.Status.pending) {
            throw new BadRequestException(
                    "Can't Confirm payment order status: " + order.getStatus());
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(order.getTotal());
        payment.setTransactionId(request.getTransactionId());
        payment.setStatus(Payment.Status.completed);

        paymentRepository.save(payment);

        order.setStatus(Order.Status.paid);
        orderRepository.save(order);

        return toResponse(payment);
    }


    private PaymentDto.Response toResponse(Payment p) {
        PaymentDto.Response res = new PaymentDto.Response();
        res.setId(p.getId());
        res.setOrderId(p.getOrder().getId());
        res.setPaymentMethod(p.getPaymentMethod());
        res.setStatus(p.getStatus().name());
        res.setAmount(p.getAmount());
        res.setTransactionId(p.getTransactionId());
        return res;
    }
}
