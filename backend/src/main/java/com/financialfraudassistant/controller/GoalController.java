package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.GoalRequest;
import com.financialfraudassistant.model.FinancialGoal;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialGoalRepository;
import com.financialfraudassistant.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final FinancialGoalRepository repo;
    private final CurrentUserService users;

    public GoalController(FinancialGoalRepository r, CurrentUserService u) { repo = r; users = u; }

    @GetMapping
    public List<FinancialGoal> list(Authentication a) {
        return repo.findByUserIdOrderByCreatedAtDesc(users.requireUser(a).getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialGoal create(Authentication a, @Valid @RequestBody GoalRequest r) {
        User u = users.requireUser(a);
        return repo.save(new FinancialGoal(u, r.name().trim(), r.targetAmount(),
                r.currentAmount() == null ? BigDecimal.ZERO : r.currentAmount(),
                r.deadline(),
                r.monthlyContribution() == null ? BigDecimal.ZERO : r.monthlyContribution()));
    }

    @PutMapping("/{id}")
    public FinancialGoal update(Authentication a, @PathVariable Integer id, @Valid @RequestBody GoalRequest r) {
        User u = users.requireUser(a);
        FinancialGoal goal = repo.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        goal.update(r.name().trim(), r.targetAmount(),
                r.currentAmount() == null ? BigDecimal.ZERO : r.currentAmount(),
                r.deadline(),
                r.monthlyContribution() == null ? BigDecimal.ZERO : r.monthlyContribution());
        return repo.save(goal);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication a, @PathVariable Integer id) {
        User u = users.requireUser(a);
        FinancialGoal goal = repo.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        repo.delete(goal);
    }
}