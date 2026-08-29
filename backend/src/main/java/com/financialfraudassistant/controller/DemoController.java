package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.DemoDataResponse;
import com.financialfraudassistant.service.CurrentUserService;
import com.financialfraudassistant.service.DemoDataService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final CurrentUserService currentUserService;
    private final DemoDataService demoDataService;

    public DemoController(CurrentUserService currentUserService, DemoDataService demoDataService) {
        this.currentUserService = currentUserService;
        this.demoDataService = demoDataService;
    }

    @PostMapping("/load-sample")
    public DemoDataResponse loadSample(Authentication authentication) {
        return demoDataService.loadSample(currentUserService.requireUser(authentication));
    }

    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.OK)
    public DemoDataResponse clear(Authentication authentication) {
        return demoDataService.clear(currentUserService.requireUser(authentication));
    }
}