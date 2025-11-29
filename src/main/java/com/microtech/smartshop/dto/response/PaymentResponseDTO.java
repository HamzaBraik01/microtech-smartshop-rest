package com.microtech.smartshop.dto.response;

import com.microtech.smartshop.enums.PaymentMethod;
import com.microtech.smartshop.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDTO {
    private String id;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private String reference;
    private PaymentMethod method;
    private PaymentStatus status;
}