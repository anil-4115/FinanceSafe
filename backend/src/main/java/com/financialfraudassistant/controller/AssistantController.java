package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.AssistantRequest;
import com.financialfraudassistant.dto.AssistantResponse;
import com.financialfraudassistant.dto.ChatMessageDto;
import com.financialfraudassistant.service.AssistantService;
import com.financialfraudassistant.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final CurrentUserService currentUserService;
    private final AssistantService assistantService;

    public AssistantController(CurrentUserService currentUserService, AssistantService assistantService) {
        this.currentUserService = currentUserService;
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public AssistantResponse chat(Authentication authentication, @Valid @RequestBody AssistantRequest request) {
        return assistantService.handle(currentUserService.requireUser(authentication), request);
    }

    @GetMapping("/history")
    public List<ChatMessageDto> history(Authentication authentication) {
        return assistantService.history(currentUserService.requireUser(authentication));
    }
}