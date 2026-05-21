package com.example.Parallel.controller;

import com.example.Parallel.entity.DailySalesReport;
import com.example.Parallel.repository.DailySalesReportRepository;
import com.example.Parallel.service.DailySalesBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final DailySalesBatchService batchService;
    private final DailySalesReportRepository reportRepository;


    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DailySalesReport> runBatch(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().minusDays(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        DailySalesReport report = batchService.processDaySales(date);
        return ResponseEntity.ok(report);
    }


    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DailySalesReport>> getAllReports() {
        return ResponseEntity.ok(reportRepository.findAllByOrderByReportDateDesc());
    }
}