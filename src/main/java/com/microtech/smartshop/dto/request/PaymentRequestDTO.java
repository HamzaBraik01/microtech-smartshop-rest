package com.microtech.smartshop.dto.request;

import com.microtech.smartshop.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotNull(message = "Le mode de paiement est requis")
    private PaymentMethod method;

    private String reference;

    private LocalDateTime paymentDate;
}