package com.microtech.smartshop.dto.response;

import com.microtech.smartshop.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponseDTO {
    private String id;
    private String orderRef;
    private LocalDateTime createdAt;
    private BigDecimal subTotal;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private List<OrderItemResponseDTO> items;
}