package com.example.Parallel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

public class ProductDto {


    @Data
    public static class Request {

        @NotBlank(message = "Product name required")
        private String name;

        private String description;

        @NotNull(message = "Price Required")
        @DecimalMin(value = "0.01", message = "Price must be higher than 0")
        private BigDecimal price;

        @Min(value = 0, message = "Stock can't be negative")
        private Integer stock = 0;
    }


    @Data
    public static class Response {
        private Integer id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
    }
}
