package com.example.Parallel.service;

import com.example.Parallel.dto.OrderDto;
import com.example.Parallel.entity.*;
import com.example.Parallel.exception.BadRequestException;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.*;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductRepository productRepository;


    @Transactional
    public OrderDto.Response createOrder(String email, OrderDto.Request request) {
        User user = userService.getUser(email);
        Order order = new Order();
        order.setUser(user);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderDto.Request.ItemRequest itemReq : request.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product with id " + itemReq.getProductId()+ " not found"));

            if (product.getStock() < itemReq.getQuantity()) {
                throw new BadRequestException(
                        "Quantity of product" + product.getName() + " is not enough"+
                                "Available: " + product.getStock() +
                                "Requested: " + itemReq.getQuantity());
            }


            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(product.getPrice());
            items.add(item);

            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity())));


            product.setStock(product.getStock() - itemReq.getQuantity());
            productRepository.save(product);
        }

        order.setItems(items);
        order.setTotal(total);
        order.setStatus(Order.Status.pending);

        return toResponse(orderRepository.save(order));
    }

    public List<OrderDto.Response> getUserOrders(String email) {
        User user = userService.getUser(email);


        return orderRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto.Response cancelOrder(Integer orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + orderId + " not found"));

        if (!order.getUser().getEmail().equals(email)) {
            throw new BadRequestException("Can't cancel order you don't own");
        }

        if (order.getStatus() != Order.Status.pending) {
            throw new BadRequestException("Can't Cancel Order, Order status: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.Status.cancelled);
        return toResponse(orderRepository.save(order));
    }


    private OrderDto.Response toResponse(Order order) {
        OrderDto.Response res = new OrderDto.Response();
        res.setId(order.getId());
        res.setStatus(order.getStatus().name());
        res.setTotal(order.getTotal());

        if (order.getItems() != null) {
            List<OrderDto.Response.ItemResponse> itemResponses = order.getItems().stream()
                    .map(item -> {
                        OrderDto.Response.ItemResponse ir = new OrderDto.Response.ItemResponse();
                        ir.setProductName(item.getProduct().getName());
                        ir.setQuantity(item.getQuantity());
                        ir.setPrice(item.getPrice());
                        return ir;
                    }).collect(Collectors.toList());
            res.setItems(itemResponses);
        }

        return res;
    }
}