package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.PaymentRequestDTO;
import com.microtech.smartshop.dto.response.PaymentResponseDTO;
import java.util.List;

public interface PaymentService {
    PaymentResponseDTO addPayment(String orderId, PaymentRequestDTO dto);
    List<PaymentResponseDTO> getPaymentsByOrder(String orderId);
}