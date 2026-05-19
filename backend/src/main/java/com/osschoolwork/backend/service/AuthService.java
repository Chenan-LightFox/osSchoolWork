package com.osschoolwork.backend.service;

import com.osschoolwork.backend.dto.AuthResponse;
import com.osschoolwork.backend.dto.LoginRequest;
import com.osschoolwork.backend.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
