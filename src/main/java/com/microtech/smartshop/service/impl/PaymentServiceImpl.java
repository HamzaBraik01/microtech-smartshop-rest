package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.PaymentRequestDTO;
import com.microtech.smartshop.dto.response.PaymentResponseDTO;
import com.microtech.smartshop.entity.Order;
import com.microtech.smartshop.entity.Payment;
import com.microtech.smartshop.enums.OrderStatus;
import com.microtech.smartshop.enums.PaymentMethod;
import com.microtech.smartshop.enums.PaymentStatus;
import com.microtech.smartshop.exception.BusinessException;
import com.microtech.smartshop.exception.ResourceNotFoundException;
import com.microtech.smartshop.mapper.PaymentMapper;
import com.microtech.smartshop.repository.OrderRepository;
import com.microtech.smartshop.repository.PaymentRepository;
import com.microtech.smartshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponseDTO addPayment(String orderId, PaymentRequestDTO dto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));

        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REJECTED) {
            throw new BusinessException("Impossible d'ajouter un paiement à une commande annulée ou rejetée");
        }

        if (order.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Cette commande est déjà totalement payée");
        }

        if (dto.getAmount().compareTo(order.getRemainingAmount()) > 0) {
            throw new BusinessException("Le montant du paiement dépasse le reste à payer (" + order.getRemainingAmount() + " DH)");
        }

        if (dto.getMethod() == PaymentMethod.ESPECES && dto.getAmount().compareTo(new BigDecimal("20000")) > 0) {
            throw new BusinessException("Le paiement en espèces est limité à 20,000 DH");
        }

        Payment payment = paymentMapper.toEntity(dto);
        payment.setOrder(order);
        payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDateTime.now());

        if (dto.getMethod() == PaymentMethod.ESPECES) {
            payment.setStatus(PaymentStatus.ENCAISSE);
            payment.setClearingDate(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.EN_ATTENTE);
        }

        int nextPaymentNumber = paymentRepository.countByOrderId(order.getId()) + 1;
        payment.setPaymentNumber(nextPaymentNumber);

        BigDecimal newRemaining = order.getRemainingAmount().subtract(dto.getAmount());
        order.setRemainingAmount(newRemaining);

        paymentRepository.save(payment);
        orderRepository.save(order);

        return paymentMapper.toDto(payment);
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }
}