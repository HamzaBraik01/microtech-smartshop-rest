package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.request.OrderItemRequestDTO;
import com.microtech.smartshop.dto.response.OrderResponseDTO;
import com.microtech.smartshop.entity.*;
import com.microtech.smartshop.enums.OrderStatus;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.mapper.OrderMapper;
import com.microtech.smartshop.repository.ClientRepository;
import com.microtech.smartshop.repository.OrderRepository;
import com.microtech.smartshop.repository.ProductRepository;
import com.microtech.smartshop.repository.UserRepository;
import com.microtech.smartshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO dto, String userIdFromSession) {
        Client client = resolveClient(dto, userIdFromSession);

        Order order = initializeOrder(client, dto.getPromoCode());

        boolean isStockInsufficient = processOrderItems(order, dto.getItems());

        finalizeOrder(order, isStockInsufficient);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }


    private Client resolveClient(CreateOrderRequestDTO dto, String userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (currentUser.getRole() == UserRole.ADMIN && dto.getClientId() != null) {
            return clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client cible introuvable"));
        } else if (currentUser.getRole() == UserRole.CLIENT) {
            return clientRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Profil client introuvable"));
        } else {
            throw new RuntimeException("Impossible d'identifier le client pour cette commande");
        }
    }


    private Order initializeOrder(Client client, String promoCode) {
        Order order = new Order();
        order.setOrderRef("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCreatedAt(LocalDateTime.now());
        order.setClient(client);
        order.setPromoCode(promoCode);
        order.setItems(new ArrayList<>());
        return order;
    }


    private boolean processOrderItems(Order order, List<OrderItemRequestDTO> itemsDto) {
        BigDecimal subTotal = BigDecimal.ZERO;
        boolean stockInsufficient = false;

        for (OrderItemRequestDTO itemDto : itemsDto) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produit introuvable : " + itemDto.getProductId()));

            if (itemDto.getQuantity() > product.getStock()) {
                stockInsufficient = true;
            }

            OrderItem orderItem = createOrderItem(order, product, itemDto.getQuantity());

            order.getItems().add(orderItem);
            subTotal = subTotal.add(orderItem.getTotalPrice());
        }

        order.setSubTotal(subTotal);

        return stockInsufficient;
    }


    private OrderItem createOrderItem(Order order, Product product, Integer quantity) {
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                .order(order)
                .build();
    }


    private void finalizeOrder(Order order, boolean isStockInsufficient) {
        if (isStockInsufficient) {
            order.setStatus(OrderStatus.REJECTED);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }

        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);

        order.setTotalAmount(order.getSubTotal());
        order.setRemainingAmount(order.getSubTotal());
    }
}