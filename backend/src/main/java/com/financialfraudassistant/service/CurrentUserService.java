package com.financialfraudassistant.service;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
@Service
public class CurrentUserService {
    private final UserRepository userRepository;
    public CurrentUserService(UserRepository userRepository) { this.userRepository = userRepository; }
    public User requireUser(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        return userRepository.findByEmailIgnoreCase(authentication.getName().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account was not found"));
    }
}
