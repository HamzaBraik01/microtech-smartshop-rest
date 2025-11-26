package com.microtech.smartshop.controller;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.dto.LoginRequest;
import com.microtech.smartshop.service.AuthService;
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
public class AuthController {
    private final AuthService authService;

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

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }
}
