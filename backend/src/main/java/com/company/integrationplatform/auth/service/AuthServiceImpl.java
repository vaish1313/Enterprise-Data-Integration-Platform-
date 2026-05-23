package com.company.integrationplatform.auth.service;

import com.company.integrationplatform.audit.AuditService;
import com.company.integrationplatform.auth.dto.LoginRequest;
import com.company.integrationplatform.auth.dto.LoginResponse;
import com.company.integrationplatform.auth.dto.RefreshTokenRequest;
import com.company.integrationplatform.auth.dto.RegisterRequest;
import com.company.integrationplatform.auth.entity.RefreshToken;
import com.company.integrationplatform.auth.repository.TokenRepository;
import com.company.integrationplatform.common.Constants;
import com.company.integrationplatform.exception.UnauthorizedException;
import com.company.integrationplatform.exception.ValidationException;
import com.company.integrationplatform.security.JwtTokenProvider;
import com.company.integrationplatform.security.UserPrincipal;
import com.company.integrationplatform.user.dto.UserDto;
import com.company.integrationplatform.user.entity.User;
import com.company.integrationplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.OPERATOR)   // default role for self-registration
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log(Constants.ACTION_REGISTER, saved.getUsername(), "SUCCESS",
                "New user registered: " + saved.getUsername());
        log.info("User registered: {}", saved.getUsername());
        return UserDto.from(saved);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken  = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(principal.getUsername());

        // Persist refresh token
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow();
        tokenRepository.deleteByUser(user);
        tokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build());

        auditService.log(Constants.ACTION_LOGIN, principal.getUsername(), "SUCCESS", "User logged in");
        log.info("User logged in: {}", principal.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(principal.getId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken stored = tokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.isExpired()) {
            tokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        User user = stored.getUser();
        String newAccessToken  = tokenProvider.generateAccessTokenFromUsername(user.getUsername());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        stored.setToken(newRefreshToken);
        stored.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        tokenRepository.save(stored);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            tokenRepository.deleteByUser(user);
            auditService.log(Constants.ACTION_LOGOUT, username, "SUCCESS", "User logged out");
            log.info("User logged out: {}", username);
        });
    }
}
