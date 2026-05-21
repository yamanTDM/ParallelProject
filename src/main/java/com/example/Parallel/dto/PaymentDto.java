package com.example.Parallel.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;


public class PaymentDto {


    @Data
    public static class Request {

        @NotNull(message = "Order Id Required")
        private Integer orderId;

        @NotBlank(message = "Payment Method Required")
        @Pattern(regexp = "cash|card|paypal", message = "Payment method must be: Cash, Card pr Paypal")
        private String paymentMethod;

        private String transactionId;
    }


    @Data
    public static class Response {
        private Integer id;
        private Integer orderId;
        private String paymentMethod;
        private String status;
        private BigDecimal amount;
        private String transactionId;
    }
}
