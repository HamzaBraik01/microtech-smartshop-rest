package com.microtech.smartshop.service.impl;

import com.microtech.smartshop.dto.request.LoginRequest;
import com.microtech.smartshop.entity.User;
import com.microtech.smartshop.repository.UserRepository;
import com.microtech.smartshop.service.AuthService;
import com.microtech.smartshop.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public User authenticate(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur introuvable");
        }

        User user = userOpt.get();

        String hashedPassword = PasswordUtil.hashPassword(loginRequest.getPassword());

        if (!hashedPassword.equals(user.getMotDePasse())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        return user;
    }
}