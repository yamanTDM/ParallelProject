package com.example.Parallel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {


    @Async("orderExecutor")
    public void sendOrderConfirmationEmail(String email, Integer orderId) {

        try {

            log.info("Starting email sending for order {}", orderId);

            Thread.sleep(5000);

            log.info("Email sent successfully to {} for order {}",
                    email,
                    orderId);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.error("Email task interrupted", e);
        }
    }


    @Async("orderExecutor")
    public void updateStatistics(Integer orderId) {

        try {

            log.info("Updating statistics for order {}", orderId);

            Thread.sleep(3000);

            log.info("Statistics updated for order {}", orderId);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.error("Statistics task interrupted", e);
        }
    }
    public void sendOrderConfirmationEmailNoAsync(String email, Integer orderId) {

        try {

            log.info("NO ASYNC | Starting email sending for order {}", orderId);

            Thread.sleep(5000);

            log.info("NO ASYNC | Email sent successfully to {} for order {}",
                    email,
                    orderId);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.error("NO ASYNC | Email task interrupted", e);
        }
    }
    public void updateStatisticsNoAsync(Integer orderId) {

        try {

            log.info("NO ASYNC | Updating statistics for order {}", orderId);

            Thread.sleep(3000);

            log.info("NO ASYNC | Statistics updated for order {}", orderId);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            log.error("NO ASYNC | Statistics task interrupted", e);
        }
    }
}