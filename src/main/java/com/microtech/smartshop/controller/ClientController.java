package com.microtech.smartshop.controller;

import com.microtech.smartshop.dto.request.ClientRequestDTO;
import com.microtech.smartshop.dto.response.ClientResponseDTO;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.enums.UserRole;
import com.microtech.smartshop.service.ClientService;
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
public class ClientController {

    private final ClientService clientService;

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("currentUser");
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getAllClients(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");
        }

        return ResponseEntity.ok(clientService.getAllClients());
    }

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

    @PostMapping
    public ResponseEntity<?> createClient(@RequestBody @Valid ClientRequestDTO dto, HttpServletRequest request) {
        User currentUser = getCurrentUser(request);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Seul un admin peut créer un client");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(dto));
    }
}