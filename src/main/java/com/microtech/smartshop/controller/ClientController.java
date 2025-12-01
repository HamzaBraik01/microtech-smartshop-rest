package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
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
}