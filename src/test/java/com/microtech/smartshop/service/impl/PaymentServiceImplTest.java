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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order testOrder;
    private PaymentRequestDTO paymentRequestDTO;
    private Payment testPayment;
    private PaymentResponseDTO paymentResponseDTO;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId("order-1");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(1000));
        testOrder.setRemainingAmount(BigDecimal.valueOf(1000));

        paymentRequestDTO = new PaymentRequestDTO();
        paymentRequestDTO.setAmount(BigDecimal.valueOf(500));
        paymentRequestDTO.setMethod(PaymentMethod.VIREMENT);

        testPayment = new Payment();
        testPayment.setId("payment-1");
        testPayment.setAmount(BigDecimal.valueOf(500));
        testPayment.setMethod(PaymentMethod.VIREMENT);
        testPayment.setStatus(PaymentStatus.EN_ATTENTE);
        testPayment.setOrder(testOrder);

        paymentResponseDTO = new PaymentResponseDTO();
        paymentResponseDTO.setId("payment-1");
        paymentResponseDTO.setAmount(BigDecimal.valueOf(500));
    }

    @Test
    void testAddPayment_Success() {
        // Given
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        PaymentResponseDTO result = paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getRemainingAmount().compareTo(BigDecimal.valueOf(500)) == 0
        ));
    }

    @Test
    void testAddPayment_OrderNotFound_ThrowsException() {
        // Given
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });
    }

    @Test
    void testAddPayment_CanceledOrder_ThrowsException() {
        // Given
        testOrder.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });

        assertTrue(exception.getMessage().contains("annulée ou rejetée"));
    }

    @Test
    void testAddPayment_RejectedOrder_ThrowsException() {
        // Given
        testOrder.setStatus(OrderStatus.REJECTED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });

        assertTrue(exception.getMessage().contains("annulée ou rejetée"));
    }

    @Test
    void testAddPayment_FullyPaidOrder_ThrowsException() {
        // Given
        testOrder.setRemainingAmount(BigDecimal.ZERO);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });

        assertTrue(exception.getMessage().contains("totalement payée"));
    }

    @Test
    void testAddPayment_ExceedsRemainingAmount_ThrowsException() {
        // Given
        testOrder.setRemainingAmount(BigDecimal.valueOf(300));
        paymentRequestDTO.setAmount(BigDecimal.valueOf(500)); // Plus que le reste
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });

        assertTrue(exception.getMessage().contains("dépasse le reste à payer"));
    }

    @Test
    void testAddPayment_CashExceedsLimit_ThrowsException() {
        // Given
        paymentRequestDTO.setMethod(PaymentMethod.ESPECES);
        paymentRequestDTO.setAmount(BigDecimal.valueOf(25000)); // Plus de 20000
        testOrder.setRemainingAmount(BigDecimal.valueOf(30000));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            paymentService.addPayment("order-1", paymentRequestDTO);
        });

        assertTrue(exception.getMessage().contains("limité à 20,000"));
    }

    @Test
    void testAddPayment_CashPayment_ImmediateClearing() {
        // Given
        paymentRequestDTO.setMethod(PaymentMethod.ESPECES);
        paymentRequestDTO.setAmount(BigDecimal.valueOf(500));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.ENCAISSE &&
                        payment.getClearingDate() != null
        ));
    }

    @Test
    void testAddPayment_CardPayment_PendingStatus() {
        // Given
        paymentRequestDTO.setMethod(PaymentMethod.VIREMENT);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.EN_ATTENTE
        ));
    }

    @Test
    void testAddPayment_CheckPayment_PendingStatus() {
        // Given
        paymentRequestDTO.setMethod(PaymentMethod.CHEQUE);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getStatus() == PaymentStatus.EN_ATTENTE
        ));
    }

    @Test
    void testAddPayment_PaymentNumberIncreases() {
        // Given
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(2); // 2 paiements existants
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getPaymentNumber() == 3 // Le 3ème paiement
        ));
    }

    @Test
    void testAddPayment_UpdatesRemainingAmount() {
        // Given
        testOrder.setRemainingAmount(BigDecimal.valueOf(1000));
        paymentRequestDTO.setAmount(BigDecimal.valueOf(300));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getRemainingAmount().compareTo(BigDecimal.valueOf(700)) == 0
        ));
    }

    @Test
    void testAddPayment_PartialPayments() {
        // Given - Premier paiement de 400 sur 1000
        testOrder.setRemainingAmount(BigDecimal.valueOf(1000));
        paymentRequestDTO.setAmount(BigDecimal.valueOf(400));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getRemainingAmount().compareTo(BigDecimal.valueOf(600)) == 0
        ));
    }

    @Test
    void testGetPaymentsByOrder_Success() {
        // Given
        Payment payment2 = new Payment();
        payment2.setId("payment-2");
        payment2.setAmount(BigDecimal.valueOf(300));

        List<Payment> payments = Arrays.asList(testPayment, payment2);

        when(paymentRepository.findByOrderId("order-1")).thenReturn(payments);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        List<PaymentResponseDTO> result = paymentService.getPaymentsByOrder("order-1");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(paymentRepository, times(1)).findByOrderId("order-1");
    }

    @Test
    void testAddPayment_WithSpecificPaymentDate() {
        // Given
        LocalDateTime specificDate = LocalDateTime.of(2025, 12, 1, 10, 0);
        paymentRequestDTO.setPaymentDate(specificDate);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(testOrder));
        when(paymentMapper.toEntity(any(PaymentRequestDTO.class))).thenReturn(testPayment);
        when(paymentRepository.countByOrderId("order-1")).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(paymentResponseDTO);

        // When
        paymentService.addPayment("order-1", paymentRequestDTO);

        // Then
        verify(paymentRepository, times(1)).save(argThat(payment ->
                payment.getPaymentDate().equals(specificDate)
        ));
    }
}

