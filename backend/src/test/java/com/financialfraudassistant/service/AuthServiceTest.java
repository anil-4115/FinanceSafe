package com.financialfraudassistant.service;

import com.financialfraudassistant.dto.AuthResponse;
import com.financialfraudassistant.dto.LoginRequest;
import com.financialfraudassistant.dto.RegisterRequest;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    private static final String SECRET = "secret-token-value";

    @Test
    void register_createsUserAndReturnsToken() {
        when(jwtService.generateToken(any(User.class))).thenReturn(SECRET);
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!123")).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("John Doe", "  JOHN@Example.COM ", "Passw0rd!123"));

        assertNotNull(response.token());
        assertEquals("John Doe", response.user().fullName());
        assertEquals("john@example.com", response.user().email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("John Doe", "john@example.com", "Passw0rd!123")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        when(jwtService.generateToken(any(User.class))).thenReturn(SECRET);
        String password = "Passw0rd!123";
        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.of(new User("john@example.com", "encoded-hash", "John Doe")));
        when(passwordEncoder.matches(password, "encoded-hash")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("JOHN@example.com", password));

        assertEquals(SECRET, response.token());
        assertEquals("John Doe", response.user().fullName());
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("john@example.com"))
                .thenReturn(Optional.of(new User("john@example.com", "encoded-hash", "John Doe")));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("john@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("ghost@example.com", "whatever123")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}