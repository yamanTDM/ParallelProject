package com.example.Parallel;

import com.example.Parallel.entity.DailySalesReport;
import com.example.Parallel.repository.ProductRepository;
import com.example.Parallel.service.DailySalesBatchService;
import com.example.Parallel.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
public class BatchTest {
    DailySalesBatchService dailySalesBatchService;


    @Autowired
    BatchTest(DailySalesBatchService dailySalesBatchService) {
        this.dailySalesBatchService = dailySalesBatchService;


    }

    @Test
    void batchTest(){


        LocalDate date = LocalDate.of(2026,5,10);


        DailySalesReport dailySalesReport1 = dailySalesBatchService.processDaySalesTest(date,1);
        DailySalesReport dailySalesReport5 = dailySalesBatchService.processDaySalesTest(date,5);
        DailySalesReport dailySalesReport10 = dailySalesBatchService.processDaySalesTest(date,10);
        DailySalesReport dailySalesReport25 = dailySalesBatchService.processDaySalesTest(date,25);
        DailySalesReport dailySalesReport50 = dailySalesBatchService.processDaySalesTest(date,50);

        System.out.println("\n========================================");
        System.out.println("Batch TEST");
        System.out.println("========================================\n");


        System.out.println("\n========================================");
        System.out.println("Chunk Size 1");
        System.out.println("========================================\n");

        System.out.println("Chunk Size: 1");
        System.out.println("Chunk count: " + dailySalesReport1.getChunksProcessed());
        System.out.println("Order count: " + dailySalesReport1.getTotalOrders());
        System.out.println("Process Time: " + dailySalesReport1.getProcessingTimeMs());
        System.out.println("\n========================================");
        System.out.println("Chunk Size 5");
        System.out.println("========================================\n");
        System.out.println("Chunk Size: 5");
        System.out.println("Chunk count: " + dailySalesReport5.getChunksProcessed());
        System.out.println("Order count: " + dailySalesReport5.getTotalOrders());
        System.out.println("Process Time: " + dailySalesReport5.getProcessingTimeMs());
        System.out.println("\n========================================");
        System.out.println("Chunk Size 10");
        System.out.println("========================================\n");
        System.out.println("Chunk Size: 10");
        System.out.println("Chunk count: " + dailySalesReport10.getChunksProcessed());
        System.out.println("Order count: " + dailySalesReport10.getTotalOrders());
        System.out.println("Process Time: " + dailySalesReport10.getProcessingTimeMs());
        System.out.println("\n========================================");
        System.out.println("Chunk Size 25");
        System.out.println("========================================\n");
        System.out.println("Chunk Size: 25");
        System.out.println("Chunk count: " + dailySalesReport25.getChunksProcessed());
        System.out.println("Order count: " + dailySalesReport25.getTotalOrders());
        System.out.println("Process Time: " + dailySalesReport25.getProcessingTimeMs());
        System.out.println("\n========================================");
        System.out.println("Chunk Size 50");
        System.out.println("========================================\n");
        System.out.println("Chunk Size: 50");
        System.out.println("Chunk count: " + dailySalesReport50.getChunksProcessed());
        System.out.println("Order count: " + dailySalesReport50.getTotalOrders());
        System.out.println("Process Time: " + dailySalesReport50.getProcessingTimeMs());
        System.out.println("\n");
    }

}
