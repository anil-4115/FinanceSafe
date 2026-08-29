package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.EducationModuleResponse;
import com.financialfraudassistant.dto.LiteracyResponse;
import com.financialfraudassistant.dto.QuizAttemptRequest;
import com.financialfraudassistant.dto.QuizAttemptResponse;
import com.financialfraudassistant.dto.QuizQuestionResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.EducationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/education")
public class EducationController {

    private final CurrentUserService currentUserService;
    private final EducationService educationService;

    public EducationController(CurrentUserService currentUserService, EducationService educationService) {
        this.currentUserService = currentUserService;
        this.educationService = educationService;
    }

    @GetMapping
    public List<EducationModuleResponse> list(Authentication authentication) {
        return educationService.list(currentUserService.requireUser(authentication));
    }

    @GetMapping("/literacy")
    public LiteracyResponse literacy(Authentication authentication) {
        return educationService.literacy(currentUserService.requireUser(authentication));
    }

    @GetMapping("/{id}")
    public EducationModuleResponse detail(Authentication authentication, @PathVariable Integer id) {
        return educationService.detail(currentUserService.requireUser(authentication), id);
    }

    @GetMapping("/{id}/quiz")
    public List<QuizQuestionResponse> quiz(Authentication authentication, @PathVariable Integer id) {
        return educationService.quiz(id);
    }

    @PostMapping("/{id}/attempt")
    public QuizAttemptResponse attempt(Authentication authentication, @PathVariable Integer id,
                                       @Valid @RequestBody QuizAttemptRequest request) {
        return educationService.attempt(currentUserService.requireUser(authentication), id, request);
    }
}