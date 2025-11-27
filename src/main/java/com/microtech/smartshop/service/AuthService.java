package com.microtech.smartshop.service;

import com.microtech.smartshop.dto.request.LoginRequest;
import com.microtech.smartshop.entity.User;

public interface AuthService {
    User authenticate(LoginRequest loginRequest);
}