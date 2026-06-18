package com.example.Parallel.service;

import com.example.Parallel.dto.CartDto;
import com.example.Parallel.dto.OrderDto;
import com.example.Parallel.dto.ProductDto;
import com.example.Parallel.entity.*;
import com.example.Parallel.exception.BadRequestException;
import com.example.Parallel.exception.ResourceNotFoundException;
import com.example.Parallel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final AsyncTaskService asyncTaskService;

    //to get or create a cart if there isn't one
    public Cart getOrCreateCart(String email) {
        User user = userService.getUser(email);

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }


    //to add an item to a cart
    @Transactional
    public CartDto.Response addItemToCart(String email, CartDto.AddItemRequest request) {
        Cart cart = getOrCreateCart(email);
        Product product = getProduct(productService.getById(request.getProductId()));


        if (product.getStock() < request.getQuantity()) {
                throw new BadRequestException("The requested quantity is not available in stock");
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new BadRequestException("The total quantity requested exceeds the stock.");
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        return mapCartResponse(cart);
    }

    //to check out cart
    @Transactional
    public OrderDto.Response checkout(String email) {
        Cart cart = getOrCreateCart(email);

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your Cart is empty, Can't checkout");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(Order.Status.pending);


        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        List<CartItem> sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .collect(Collectors.toList());

        for (CartItem cartItem :sortedItems) {

            Product product = getProduct(productService.getById(cartItem.getProduct().getId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            //Synchronization Point
            productService.updateStock(product.getId(),cartItem.getQuantity());
        }

        order.setItems(orderItems);
        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);
        asyncTaskService.sendOrderConfirmationEmail(
                order.getUser().getEmail(),
                savedOrder.getId()
        );

        asyncTaskService.updateStatistics(
                savedOrder.getId()
        );
        cart.getItems().clear();
        cartRepository.save(cart);

        return mapToOrderResponse(savedOrder);
    }

    public CartDto.Response getCartResponse(String email) {
        Cart cart = getOrCreateCart(email);
        CartDto.Response response = new CartDto.Response();
        response.setId(cart.getId());

        BigDecimal cartTotal = BigDecimal.ZERO;
        List<CartDto.Response.ItemResponse> itemResponses = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            CartDto.Response.ItemResponse itemRes = new CartDto.Response.ItemResponse();
            itemRes.setId(item.getId());
            itemRes.setProductId(item.getProduct().getId());
            itemRes.setProductName(item.getProduct().getName());
            itemRes.setQuantity(item.getQuantity());
            itemRes.setPrice(item.getProduct().getPrice());

            BigDecimal subTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemRes.setSubTotal(subTotal);
            itemResponses.add(itemRes);

            cartTotal = cartTotal.add(subTotal);
        }

        response.setItems(itemResponses);
        response.setCartTotal(cartTotal);
        return response;
    }
    private CartDto.Response mapCartResponse(Cart cart) {
        CartDto.Response response = new CartDto.Response();
        response.setId(cart.getId());

        BigDecimal cartTotal = BigDecimal.ZERO;
        List<CartDto.Response.ItemResponse> itemResponses = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            CartDto.Response.ItemResponse itemRes = new CartDto.Response.ItemResponse();
            itemRes.setId(item.getId());
            itemRes.setProductId(item.getProduct().getId());
            itemRes.setProductName(item.getProduct().getName());
            itemRes.setQuantity(item.getQuantity());
            itemRes.setPrice(item.getProduct().getPrice());

            BigDecimal subTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemRes.setSubTotal(subTotal);
            itemResponses.add(itemRes);

            cartTotal = cartTotal.add(subTotal);
        }

        response.setItems(itemResponses);
        response.setCartTotal(cartTotal);
        return response;
    }

    private OrderDto.Response mapToOrderResponse(Order order) {
        OrderDto.Response res = new OrderDto.Response();
        res.setId(order.getId());
        res.setStatus(order.getStatus().name());
        res.setTotal(order.getTotal());
        return res;
    }

    //to remove item from cart
    @Transactional
    public CartDto.Response removeItemFromCart(String email, Integer productId) {
        Cart cart = getOrCreateCart(email);

        boolean removed = cart.getItems().removeIf(item ->
                item.getProduct().getId().equals(productId)
        );

        if (!removed) {
            throw new ResourceNotFoundException("Product is not in Cart");
        }

        cartRepository.save(cart);
        return mapCartResponse(cart);
    }

    public Product getProduct(ProductDto.Response response) {
        Product product = new Product();
        product.setId(response.getId());
        product.setName(response.getName());
        product.setPrice(response.getPrice());
        product.setStock(response.getStock());
        product.setDescription(response.getDescription());
        return product;

    }

}