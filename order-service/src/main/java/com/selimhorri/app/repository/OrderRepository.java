package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.selimhorri.app.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findAllByIsActiveTrue();

    Optional<Order> findByOrderIdAndIsActiveTrue(Integer orderId);
}