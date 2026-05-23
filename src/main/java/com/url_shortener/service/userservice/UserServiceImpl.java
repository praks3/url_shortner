package com.url_shortener.service.userservice;

import com.url_shortener.dto.auth.AuthResponse;
import com.url_shortener.dto.auth.LoginRequest;
import com.url_shortener.dto.auth.RegisterRequest;
import com.url_shortener.entity.User;
import com.url_shortener.exception.UsernameAlreadyExistsException;
import com.url_shortener.exception.EmailAlreadyExistsException;
import com.url_shortener.exception.InvalidCredentialsException;
import com.url_shortener.repository.UserRepository;
import com.url_shortener.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);

        String token = jwtService.issue(user.getUsername(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .expiresInMs(jwtService.expirationMs())
                .username(user.getUsername())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String token = jwtService.issue(user.getUsername(), user.getId());
        return AuthResponse.builder()
                .token(token)
                .expiresInMs(jwtService.expirationMs())
                .username(user.getUsername())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElse(null);
    }
}
