package com.example.Parallel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "daily_sales_reports")
public class DailySalesReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "report_date", nullable = false)
    private String reportDate;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_units_sold")
    private Integer totalUnitsSold;

    @Column(name = "chunks_processed")
    private Integer chunksProcessed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt = LocalDateTime.now();
}