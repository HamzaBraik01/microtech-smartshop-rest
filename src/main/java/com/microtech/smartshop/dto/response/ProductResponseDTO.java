package com.microtech.smartshop.dto.response;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductResponseDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Boolean isDeleted;
}
