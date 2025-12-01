package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.request.OrderItemRequestDTO;
import com.microtech.smartshop.dto.response.OrderResponseDTO;
import com.microtech.smartshop.entity.*;
import com.microtech.smartshop.enums.CustomerTier;
import com.microtech.smartshop.enums.OrderStatus;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.mapper.OrderMapper;
import com.microtech.smartshop.repository.ClientRepository;
import com.microtech.smartshop.repository.OrderRepository;
import com.microtech.smartshop.repository.ProductRepository;
import com.microtech.smartshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Client testClient;
    private Product testProduct;
    private CreateOrderRequestDTO orderRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .username("testclient")
                .role(UserRole.CLIENT)
                .build();

        testClient = Client.builder()
                .id("client-1")
                .nom("Test Client")
                .email("test@example.com")
                .fidelityLevel(CustomerTier.BASIC)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .user(testUser)
                .build();

        testProduct = Product.builder()
                .id("product-1")
                .name("Test Product")
                .price(BigDecimal.valueOf(100))
                .stock(50)
                .isDeleted(false)
                .build();

        OrderItemRequestDTO itemRequest = new OrderItemRequestDTO();
        itemRequest.setProductId("product-1");
        itemRequest.setQuantity(5);

        orderRequest = new CreateOrderRequestDTO();
        orderRequest.setItems(Arrays.asList(itemRequest));
    }

    @Test
    void testCreateOrder_BasicClient_NoDiscount() {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));

        Order savedOrder = new Order();
        savedOrder.setId("order-1");
        savedOrder.setSubTotal(BigDecimal.valueOf(500));
        savedOrder.setDiscountAmount(BigDecimal.ZERO);
        savedOrder.setTaxAmount(BigDecimal.valueOf(100));
        savedOrder.setTotalAmount(BigDecimal.valueOf(600));
        savedOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(productRepository, times(1)).findById("product-1");
    }

    @Test
    void testCreateOrder_SilverClient_WithDiscount() {
        testClient.setFidelityLevel(CustomerTier.SILVER);

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            BigDecimal expectedDiscount = BigDecimal.valueOf(25.00).setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedDiscount, order.getDiscountAmount());
            return order;
        });

        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_GoldClient_WithDiscount() {
        testClient.setFidelityLevel(CustomerTier.GOLD);

        orderRequest.getItems().get(0).setQuantity(10); // 10 * 100 = 1000

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            BigDecimal expectedDiscount = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedDiscount, order.getDiscountAmount());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_PlatinumClient_WithDiscount() {
        testClient.setFidelityLevel(CustomerTier.PLATINUM);

        orderRequest.getItems().get(0).setQuantity(15); // 15 * 100 = 1500

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            BigDecimal expectedDiscount = BigDecimal.valueOf(225.00).setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedDiscount, order.getDiscountAmount());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_WithPromoCode_AdditionalDiscount() {
        testClient.setFidelityLevel(CustomerTier.SILVER);
        orderRequest.setPromoCode("PROMO-ABC1");
        orderRequest.getItems().get(0).setQuantity(6); // 6 * 100 = 600 (> 500 pour remise SILVER)

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            BigDecimal expectedDiscount = BigDecimal.valueOf(60.00).setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedDiscount, order.getDiscountAmount());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_InsufficientStock_OrderRejected() {
        orderRequest.getItems().get(0).setQuantity(100);

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertEquals(OrderStatus.REJECTED, order.getStatus());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_SufficientStock_OrderPending() {
        orderRequest.getItems().get(0).setQuantity(10);

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            assertEquals(OrderStatus.PENDING, order.getStatus());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrder_TaxCalculation() {
        orderRequest.getItems().get(0).setQuantity(10); // 10 * 100 = 1000

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(clientRepository.findByUserId(anyString())).thenReturn(Optional.of(testClient));
        when(productRepository.findById(anyString())).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            BigDecimal expectedTax = BigDecimal.valueOf(200.00).setScale(2, RoundingMode.HALF_UP);
            assertEquals(expectedTax, order.getTaxAmount());
            return order;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.createOrder(orderRequest, "user-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testConfirmOrder_Success() {
        Order order = new Order();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);
        order.setRemainingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(600));
        order.setClient(testClient);
        order.setItems(new ArrayList<>());

        OrderItem item = OrderItem.builder()
                .product(testProduct)
                .quantity(5)
                .build();
        order.getItems().add(item);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            assertEquals(OrderStatus.CONFIRMED, o.getStatus());
            return o;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            assertEquals(45, p.getStock()); // 50 - 5
            return p;
        });
        when(clientRepository.save(any(Client.class))).thenReturn(testClient);
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        OrderResponseDTO result = orderService.confirmOrder("order-1");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testConfirmOrder_WithUnpaidAmount_ThrowsException() {
        Order order = new Order();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);
        order.setRemainingAmount(BigDecimal.valueOf(100));

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.confirmOrder("order-1"));

        assertTrue(exception.getMessage().contains("n'est pas totalement payée"));
    }

    @Test
    void testConfirmOrder_NonPendingOrder_ThrowsException() {
        Order order = new Order();
        order.setId("order-1");
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.confirmOrder("order-1"));

        assertTrue(exception.getMessage().contains("Seules les commandes PENDING"));
    }

    @Test
    void testConfirmOrder_UpdatesClientStatistics() {
        Order order = new Order();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);
        order.setRemainingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(600));
        order.setClient(testClient);
        order.setItems(new ArrayList<>());

        OrderItem item = OrderItem.builder()
                .product(testProduct)
                .quantity(5)
                .build();
        order.getItems().add(item);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            assertEquals(1, client.getTotalOrders());
            assertEquals(0, client.getTotalSpent().compareTo(BigDecimal.valueOf(600)));
            return client;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        orderService.confirmOrder("order-1");

        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    void testConfirmOrder_ClientBecomesGold() {
        testClient.setTotalOrders(9);
        testClient.setTotalSpent(BigDecimal.valueOf(4500));
        testClient.setFidelityLevel(CustomerTier.SILVER);

        Order order = new Order();
        order.setId("order-1");
        order.setStatus(OrderStatus.PENDING);
        order.setRemainingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(600));
        order.setClient(testClient);
        order.setItems(new ArrayList<>());

        OrderItem item = OrderItem.builder()
                .product(testProduct)
                .quantity(5)
                .build();
        order.getItems().add(item);

        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            assertEquals(CustomerTier.GOLD, client.getFidelityLevel());
            return client;
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(new OrderResponseDTO());

        orderService.confirmOrder("order-1");

        verify(clientRepository, times(1)).save(any(Client.class));
    }
}

