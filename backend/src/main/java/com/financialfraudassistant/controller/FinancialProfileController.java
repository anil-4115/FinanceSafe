package com.financialfraudassistant.controller;
import com.financialfraudassistant.dto.FinancialProfileRequest;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.FinancialProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/profile")
public class FinancialProfileController {
    private final CurrentUserService currentUserService; private final FinancialProfileService profileService;
    public FinancialProfileController(CurrentUserService currentUserService, FinancialProfileService profileService) { this.currentUserService = currentUserService; this.profileService = profileService; }
    @GetMapping public FinancialProfile get(Authentication authentication) { return profileService.get(currentUserService.requireUser(authentication)); }
    @PutMapping public FinancialProfile save(Authentication authentication, @Valid @RequestBody FinancialProfileRequest request) { return profileService.save(currentUserService.requireUser(authentication), request); }
}
