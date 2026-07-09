package com.urlshortener.auth.service;

import com.urlshortener.auth.dto.*;
import com.urlshortener.auth.entity.User;
import com.urlshortener.auth.repository.UserRepository;
import com.urlshortener.common.exception.ConflictException;
import com.urlshortener.common.exception.ResourceNotFoundException;
import com.urlshortener.common.exception.UnauthorizedException;
import com.urlshortener.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            if (!jwtService.isRefreshToken(request.getRefreshToken())) {
                throw new UnauthorizedException("Invalid refresh token");
            }
            UUID userId = jwtService.getUserId(request.getRefreshToken());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));
            return buildAuthResponse(user);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .accessToken(jwtService.generateAccessToken(user.getId(), user.getEmail()))
                .refreshToken(jwtService.generateRefreshToken(user.getId(), user.getEmail()))
                .build();
    }
}
