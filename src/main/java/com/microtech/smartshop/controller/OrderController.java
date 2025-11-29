package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.request.PaymentRequestDTO;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.service.OrderService;
import com.microtech.smartshop.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateOrderRequestDTO dto, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Veuillez vous connecter");
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(orderService.createOrder(dto, currentUser.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmOrder(@PathVariable String id, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul un admin peut valider les commandes");
        }

        try {
            return ResponseEntity.ok(orderService.confirmOrder(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<?> addPayment(@PathVariable String id, @RequestBody @Valid PaymentRequestDTO dto, HttpServletRequest request) {


        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(paymentService.addPayment(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<?> getOrderPayments(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(id));
    }
}