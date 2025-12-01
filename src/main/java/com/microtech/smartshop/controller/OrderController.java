package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.CreateOrderRequestDTO;
import com.microtech.smartshop.dto.request.PaymentRequestDTO;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.service.OrderService;
import com.microtech.smartshop.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Orders", description = "Gestion des commandes et paiements")
@SecurityRequirement(name = "session")
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

    @Operation(
            summary = "Créer une nouvelle commande",
            description = "Permet à un client authentifié de passer une commande avec plusieurs produits"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commande créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou stock insuffisant"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
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


    @Operation(
            summary = "Confirmer/Valider une commande",
            description = "Permet à un ADMIN de valider une commande en attente. Change le statut de PENDING à CONFIRMED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande confirmée"),
            @ApiResponse(responseCode = "400", description = "Erreur de validation"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant)"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
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

    @Operation(
            summary = "Ajouter un paiement à une commande",
            description = "Enregistre un paiement (partiel ou total) pour une commande"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paiement enregistré"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée")
    })
    @PostMapping("/{id}/payments")
    public ResponseEntity<?> addPayment(@PathVariable String id, @RequestBody @Valid PaymentRequestDTO dto, HttpServletRequest request) {


        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(paymentService.addPayment(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Consulter les paiements d'une commande",
            description = "Récupère la liste de tous les paiements effectués pour une commande"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des paiements récupérée"),
            @ApiResponse(responseCode = "404", description = "Commande non trouvée")
    })
    @GetMapping("/{id}/payments")
    public ResponseEntity<?> getOrderPayments(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(id));
    }
}