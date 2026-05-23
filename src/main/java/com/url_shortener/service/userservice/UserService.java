package com.url_shortener.service.userservice;

import com.url_shortener.dto.auth.AuthResponse;
import com.url_shortener.dto.auth.LoginRequest;
import com.url_shortener.dto.auth.RegisterRequest;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    Long findIdByUsername(String username);
}
