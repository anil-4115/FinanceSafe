package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.AuthResponse;
import com.financialfraudassistant.dto.ForgotPasswordResponse;
import com.financialfraudassistant.dto.LoginRequest;
import com.financialfraudassistant.dto.RegisterRequest;
import com.financialfraudassistant.dto.ResetPasswordRequest;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.allow-email-free-reset:false}")
    private boolean allowEmailFreeReset;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()), request.fullName().trim()));
        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normaliseEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.expirationSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail()));
    }

    public ForgotPasswordResponse forgotPassword(String email) {
        String resetToken = null;
        if (allowEmailFreeReset) {
            resetToken = userRepository.findByEmailIgnoreCase(normaliseEmail(email))
                    .map(jwtService::generateResetToken)
                    .orElse(null);
        }
        return new ForgotPasswordResponse(
                "If that email matches an account, a password reset token has been generated. In production this would be emailed to you.",
                resetToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normaliseEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));
        if (!jwtService.isValidResetToken(user, request.token())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private String normaliseEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
