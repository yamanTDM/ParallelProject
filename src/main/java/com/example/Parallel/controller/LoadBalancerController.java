package com.example.Parallel.controller;

import com.example.Parallel.service.LoadBalancerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/lb")
public class LoadBalancerController {

    private final LoadBalancerService lbService;

    public LoadBalancerController(LoadBalancerService lbService) {
        this.lbService = lbService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
            "strategy",  "ROUND_ROBIN",
            "instances", lbService.getInstancesStatus()
        ));
    }
}
