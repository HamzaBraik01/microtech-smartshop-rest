package com.microtech.smartshop.repository;

import com.microtech.smartshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    boolean existsByProductId(String productId);
}