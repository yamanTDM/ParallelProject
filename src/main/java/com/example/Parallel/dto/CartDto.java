package com.example.Parallel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class CartDto {

    @Data
    public static class AddItemRequest {
        @NotNull(message = "id Required")
        private Integer productId;

        @Min(value = 1, message = "Quantity has to be at least 1")
        private Integer quantity;
    }

    @Data
    public static class Response {
        private Integer id;
        private List<ItemResponse> items;
        private BigDecimal cartTotal;
        @Data
        public static class ItemResponse {
            private Integer id;
            private Integer productId;
            private String productName;
            private Integer quantity;
            private BigDecimal price;
            private BigDecimal subTotal;
        }
    }
}