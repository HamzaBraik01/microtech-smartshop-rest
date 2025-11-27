package com.microtech.smartshop.dto.response;

import com.microtech.smartshop.enums.CustomerTier;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClientResponseDTO {
    private String id;
    private String nom;
    private String email;
    private String telephone;
    private CustomerTier fidelityLevel;
    private Integer totalOrders;
    private BigDecimal totalSpent;
}
