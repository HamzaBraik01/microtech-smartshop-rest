package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.request.ClientUpdateDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.service.ClientService;
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

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Gestion des clients")
@SecurityRequirement(name = "session")
public class ClientController {

    private final ClientService clientService;

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    @Operation(
            summary = "Lister tous les clients",
            description = "Récupère la liste complète des clients. Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des clients récupérée"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant)"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @GetMapping
    public ResponseEntity<?> getAllClients(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");
        }

        return ResponseEntity.ok(clientService.getAllClients());
    }

    @Operation(
            summary = "Consulter un client par ID",
            description = "Récupère les détails d'un client. ADMIN peut voir tous les clients, CLIENT ne peut voir que son propre profil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client trouvé"),
            @ApiResponse(responseCode = "403", description = "Accès interdit"),
            @ApiResponse(responseCode = "404", description = "Client non trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getClientById(@PathVariable String id, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ClientResponseDTO clientDTO = clientService.getClientById(id);

        if (currentUser.getRole() == UserRole.CLIENT) {
            ClientResponseDTO myProfile = clientService.getClientByUserId(currentUser.getId());
            if (!myProfile.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Vous ne pouvez consulter que votre profil");
            }
        }

        return ResponseEntity.ok(clientDTO);
    }

    @Operation(
            summary = "Créer un nouveau client",
            description = "Crée un nouveau client avec un compte utilisateur associé. Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle insuffisant)"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody @Valid ClientRequestDTO dto, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul un admin peut créer un client");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(dto));
    }

    @Operation(
            summary = "Mettre à jour un client ⭐ NOUVEAU",
            description = "Met à jour les informations d'un client (nom, email, téléphone). Vérifie l'unicité de l'email. Réservé aux ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Client mis à jour avec succès",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"id\":\"uuid-123\",\"nom\":\"Client Modifié SARL\",\"email\":\"nouveau@email.ma\",\"telephone\":\"0612349999\",\"fidelityLevel\":\"GOLD\",\"totalOrders\":5,\"totalSpent\":7500.00,\"firstOrderDate\":\"2024-01-15T10:30:00\",\"lastOrderDate\":\"2025-12-08T14:20:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":400,\"error\":\"Validation Error\",\"message\":\"email: Format d'email invalide\",\"path\":\"/api/clients/uuid-123\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (rôle insuffisant)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "\"Seul un admin peut modifier un client\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client non trouvé",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":404,\"error\":\"Resource Not Found\",\"message\":\"Client introuvable\",\"path\":\"/api/clients/uuid-123\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Email déjà utilisé par un autre client",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":422,\"error\":\"Business Rule Violation\",\"message\":\"Email déjà utilisé par un autre client\",\"path\":\"/api/clients/uuid-123\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Informations du client à mettre à jour",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "Exemple de mise à jour",
                            value = "{\"nom\":\"Client Modifié SARL\",\"email\":\"nouveau@email.ma\",\"telephone\":\"0612349999\"}"
                    )
            )
    )
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable String id,
                                           @RequestBody @Valid ClientUpdateDTO dto,
                                           HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul un admin peut modifier un client");
        }

        return ResponseEntity.ok(clientService.updateClient(id, dto));
    }

    @Operation(
            summary = "Supprimer un client ⭐ NOUVEAU",
            description = "Supprime définitivement un client et son compte utilisateur associé. " +
                    "Impossible si le client a des commandes existantes (règle métier). " +
                    "Réservé aux ADMIN uniquement."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Client supprimé avec succès (No Content)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (rôle insuffisant)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "\"Seul un admin peut supprimer un client\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client non trouvé",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":404,\"error\":\"Resource Not Found\",\"message\":\"Client introuvable\",\"path\":\"/api/clients/uuid-123\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Client a des commandes existantes (suppression impossible)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Exemple d'erreur",
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":422,\"error\":\"Business Rule Violation\",\"message\":\"Impossible de supprimer un client ayant des commandes existantes. Le client a 3 commande(s).\",\"path\":\"/api/clients/uuid-123\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable String id, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul un admin peut supprimer un client");
        }

        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Consulter l'historique des commandes d'un client ⭐ NOUVEAU",
            description = "Récupère la liste complète des commandes effectuées par un client avec les détails : " +
                    "identifiant, référence, date de création, montant total TTC et statut. " +
                    "ADMIN peut voir l'historique de tous les clients. " +
                    "CLIENT ne peut voir que ses propres commandes (vérification automatique)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Historique des commandes récupéré avec succès",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Exemple d'historique",
                                    value = "[" +
                                            "{\"id\":\"uuid-order-1\",\"orderRef\":\"ORD-20251205-001\",\"createdAt\":\"2025-12-05T10:30:00\",\"totalAmount\":5400.00,\"status\":\"CONFIRMED\"}," +
                                            "{\"id\":\"uuid-order-2\",\"orderRef\":\"ORD-20251208-002\",\"createdAt\":\"2025-12-08T14:20:00\",\"totalAmount\":2100.00,\"status\":\"PENDING\"}," +
                                            "{\"id\":\"uuid-order-3\",\"orderRef\":\"ORD-20251209-003\",\"createdAt\":\"2025-12-09T09:15:00\",\"totalAmount\":1850.00,\"status\":\"CONFIRMED\"}" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (CLIENT tente de voir l'historique d'un autre client)",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "\"Vous ne pouvez consulter que votre propre historique\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client non trouvé",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "{\"timestamp\":\"2025-12-09T10:30:00\",\"status\":404,\"error\":\"Resource Not Found\",\"message\":\"Client introuvable\",\"path\":\"/api/clients/uuid-123/orders\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    value = "\"Veuillez vous connecter\""
                            )
                    )
            )
    })
    @GetMapping("/{id}/orders")
    public ResponseEntity<?> getClientOrders(@PathVariable String id, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Veuillez vous connecter");
        }

        // CLIENT ne peut voir que ses propres commandes
        if (currentUser.getRole() == UserRole.CLIENT) {
            ClientResponseDTO myProfile = clientService.getClientByUserId(currentUser.getId());
            if (!myProfile.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Vous ne pouvez consulter que votre propre historique");
            }
        }

        return ResponseEntity.ok(clientService.getClientOrderHistory(id));
    }
}