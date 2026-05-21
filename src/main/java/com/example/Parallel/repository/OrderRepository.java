package com.example.Parallel.repository;

import com.example.Parallel.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByUserId(Integer userId);



    @Query("SELECT o FROM Order o WHERE o.status = com.example.Parallel.entity.Order.Status.paid " +
            "AND o.createdAt BETWEEN :start AND :end")
    Page<Order> findPaidOrdersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );


    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = com.example.Parallel.entity.Order.Status.paid " +
            "AND o.createdAt BETWEEN :start AND :end")
    long countPaidOrdersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}