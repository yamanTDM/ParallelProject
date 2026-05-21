package com.example.Parallel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;


public class OrderDto {


    @Data
    public static class Request {

        @NotEmpty(message = "Order must have at least one item")
        private List<ItemRequest> items;

        @Data
        public static class ItemRequest {
            @NotNull(message = "Product Id required")
            private Integer productId;

            @Min(value = 1, message = "Product quantity has to be at least 1")
            private Integer quantity;
        }
    }


    @Data
    public static class Response {
        private Integer id;
        private String status;
        private BigDecimal total;
        private List<ItemResponse> items;

        @Data
        public static class ItemResponse {
            private String productName;
            private Integer quantity;
            private BigDecimal price;
        }
    }
}
