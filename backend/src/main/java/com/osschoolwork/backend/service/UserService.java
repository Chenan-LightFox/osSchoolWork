package com.osschoolwork.backend.service;

import com.osschoolwork.backend.dto.AuthResponse;
import com.osschoolwork.backend.dto.LoginRequest;
import com.osschoolwork.backend.dto.RegisterRequest;
import com.osschoolwork.backend.dto.UserView;
import com.osschoolwork.backend.entity.User;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserView getUserViewById(Long userId);

    User findByEmail(String email);
}
