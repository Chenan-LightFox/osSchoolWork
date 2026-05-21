package com.osschoolwork.backend.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import com.osschoolwork.backend.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头读取 Bearer token
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }
        String token = auth.substring(7);
        // 解析 userId，失败则拒绝访问
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }
        // 把 userId 写入请求上下文，供后续使用
        request.setAttribute("userId", userId);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Unauthorized\",\"data\":null}");
        } catch (Exception ignored) {
        }
    }
}
