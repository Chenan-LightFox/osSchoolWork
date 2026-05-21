package com.osschoolwork.backend.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.osschoolwork.backend.dto.AuthResponse;
import com.osschoolwork.backend.dto.LoginRequest;
import com.osschoolwork.backend.dto.RegisterRequest;
import com.osschoolwork.backend.dto.UserView;
import com.osschoolwork.backend.entity.User;
import com.osschoolwork.backend.exception.BusinessException;
import com.osschoolwork.backend.mapper.UserMapper;
import com.osschoolwork.backend.service.UserService;
import com.osschoolwork.backend.util.JwtUtil;
import com.osschoolwork.backend.util.PasswordUtil;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 邮箱唯一校验
        if (findByEmail(request.getEmail()) != null) {
            throw new BusinessException(400, "Email already exists");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        // 密码加密存储
        user.setPassword(PasswordUtil.hash(request.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        // 注册完成后签发 JWT
        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponse(token, toUserView(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 账号校验
        User user = findByEmail(request.getEmail());
        if (user == null) {
            throw new BusinessException(401, "Invalid credentials");
        }
        // 密码校验
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid credentials");
        }
        // 登录成功后签发 JWT
        String token = jwtUtil.generateToken(user.getId());
        return new AuthResponse(token, toUserView(user));
    }

    @Override
    public UserView getUserViewById(Long userId) {
        // 仅返回脱敏视图字段
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return toUserView(user);
    }

    @Override
    public User findByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userMapper.selectOne(wrapper);
    }

    private UserView toUserView(User user) {
        return new UserView(user.getId(), user.getEmail(), user.getUsername());
    }
}
