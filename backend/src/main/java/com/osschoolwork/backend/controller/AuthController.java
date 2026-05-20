package com.osschoolwork.backend.controller;

import com.osschoolwork.backend.common.ApiResponse;
import com.osschoolwork.backend.dto.AuthResponse;
import com.osschoolwork.backend.dto.LoginRequest;
import com.osschoolwork.backend.dto.RegisterRequest;
import com.osschoolwork.backend.dto.UserView;
import com.osschoolwork.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/auth/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @GetMapping("/auth/me")
    public ApiResponse<UserView> me(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error(401, "Unauthorized");
        }
        return ApiResponse.success(userService.getUserViewById((Long) userId));
    }
}
