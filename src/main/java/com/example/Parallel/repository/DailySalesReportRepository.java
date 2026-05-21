package com.example.Parallel.repository;

import com.example.Parallel.entity.DailySalesReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySalesReportRepository extends JpaRepository<DailySalesReport, Integer> {

    Optional<DailySalesReport> findByReportDate(String reportDate);
    List<DailySalesReport> findAllByOrderByReportDateDesc();
}