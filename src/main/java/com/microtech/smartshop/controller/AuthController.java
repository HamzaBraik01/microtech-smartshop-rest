package com.microtech.smartshop.controller;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.dto.request.LoginRequest;
import com.microtech.smartshop.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints pour l'authentification des utilisateurs")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur et crée une session HTTP. Utilisez admin/admin123 ou techsolutions/client123"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"Connexion réussie\",\"role\":\"ADMIN\",\"username\":\"admin\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Identifiants invalides",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\":\"Échec de l'authentification\",\"message\":\"Identifiants invalides\"}")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest, HttpServletRequest request) {
        try {
            User user = authService.authenticate(loginRequest);

            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Connexion réussie");
            response.put("role", user.getRole());
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Échec de l'authentification", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Déconnexion utilisateur",
            description = "Invalide la session HTTP courante"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Déconnexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\":\"Déconnexion réussie\"}")
                    )
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}
