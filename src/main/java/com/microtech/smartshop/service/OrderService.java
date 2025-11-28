package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.response.OrderResponseDTO;

public interface OrderService {
    OrderResponseDTO createOrder(CreateOrderRequestDTO dto, String userIdFromSession);
    OrderResponseDTO confirmOrder(String orderId);
}