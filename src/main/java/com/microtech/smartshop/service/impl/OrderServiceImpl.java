package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.request.OrderItemRequestDTO;
import com.microtech.smartshop.dto.response.OrderResponseDTO;
import com.microtech.smartshop.entity.*;
import com.microtech.smartshop.enums.CustomerTier;
import com.microtech.smartshop.enums.OrderStatus;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.exception.BusinessException;
import com.microtech.smartshop.exception.ResourceNotFoundException;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    private static final BigDecimal TVA_RATE = new BigDecimal("0.20");
    private static final Pattern PROMO_PATTERN = Pattern.compile("PROMO-[A-Z0-9]{4}");

    @Override
    @Transactional
    public OrderResponseDTO createOrder(CreateOrderRequestDTO dto, String userIdFromSession) {
        Client client = resolveClient(dto, userIdFromSession);
        Order order = initializeOrder(client, dto.getPromoCode());
        boolean isStockInsufficient = processOrderItems(order, dto.getItems());

        calculateFinancials(order, client);

        finalizeStatus(order, isStockInsufficient);

        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDTO confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande introuvable"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Seules les commandes PENDING peuvent être confirmées");
        }
        if (order.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Impossible de valider : La commande n'est pas totalement payée. Reste dû : " + order.getRemainingAmount());
        }

        order.setStatus(OrderStatus.CONFIRMED);

        updateProductStocks(order);

        updateClientStatistics(order.getClient(), order.getTotalAmount());

        orderRepository.save(order);
        return orderMapper.toDto(order);
    }


    private void calculateFinancials(Order order, Client client) {
        BigDecimal subTotal = order.getSubTotal();
        BigDecimal discountRate = BigDecimal.ZERO;


        if (client.getFidelityLevel() == CustomerTier.SILVER && subTotal.compareTo(BigDecimal.valueOf(500)) > 0) {
            discountRate = discountRate.add(new BigDecimal("0.05"));
        }
        else if (client.getFidelityLevel() == CustomerTier.GOLD && subTotal.compareTo(BigDecimal.valueOf(800)) > 0) {
            discountRate = discountRate.add(new BigDecimal("0.10"));
        }
        else if (client.getFidelityLevel() == CustomerTier.PLATINUM && subTotal.compareTo(BigDecimal.valueOf(1200)) > 0) {
            discountRate = discountRate.add(new BigDecimal("0.15"));
        }

        if (order.getPromoCode() != null && PROMO_PATTERN.matcher(order.getPromoCode()).matches()) {
            discountRate = discountRate.add(new BigDecimal("0.05"));
        }

        BigDecimal discountAmount = subTotal.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountAfterDiscount = subTotal.subtract(discountAmount);

        BigDecimal taxAmount = amountAfterDiscount.multiply(TVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = amountAfterDiscount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        order.setDiscountAmount(discountAmount);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        order.setRemainingAmount(totalAmount);
    }

    private void updateClientStatistics(Client client, BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();

        if (client.getFirstOrderDate() == null) {
            client.setFirstOrderDate(now);
        }

        client.setLastOrderDate(now);

        client.setTotalOrders(client.getTotalOrders() + 1);
        client.setTotalSpent(client.getTotalSpent().add(orderTotal));

        int orders = client.getTotalOrders();
        BigDecimal spent = client.getTotalSpent();

        if (orders >= 20 || spent.compareTo(BigDecimal.valueOf(15000)) >= 0) {
            client.setFidelityLevel(CustomerTier.PLATINUM);
        } else if (orders >= 10 || spent.compareTo(BigDecimal.valueOf(5000)) >= 0) {
            client.setFidelityLevel(CustomerTier.GOLD);
        } else if (orders >= 3 || spent.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            client.setFidelityLevel(CustomerTier.SILVER);
        }

        clientRepository.save(client);
    }

    private void updateProductStocks(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0) throw new RuntimeException("Stock insuffisant pour validation : " + product.getName());
            product.setStock(newStock);
            productRepository.save(product);
        }
    }



    private void finalizeStatus(Order order, boolean isStockInsufficient) {
        if (isStockInsufficient) {
            order.setStatus(OrderStatus.REJECTED);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }
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
        }
        throw new RuntimeException("Impossible d'identifier le client");
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
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
            if (itemDto.getQuantity() > product.getStock()) stockInsufficient = true;
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())))
                    .order(order).build();
            order.getItems().add(orderItem);
            subTotal = subTotal.add(orderItem.getTotalPrice());
        }
        order.setSubTotal(subTotal);
        return stockInsufficient;
    }
}