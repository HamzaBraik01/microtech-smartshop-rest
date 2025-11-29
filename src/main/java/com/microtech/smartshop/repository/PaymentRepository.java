package com.microtech.smartshop.repository;

import com.microtech.smartshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByOrderId(String orderId);

    int countByOrderId(String orderId);
}