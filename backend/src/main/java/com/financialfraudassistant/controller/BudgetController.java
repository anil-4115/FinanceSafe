package com.financialfraudassistant.controller;

import com.financialfraudassistant.dto.BudgetRequest;
import com.financialfraudassistant.model.Budget;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.BudgetRepository;
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

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetRepository repo;
    private final CurrentUserService users;

    public BudgetController(BudgetRepository r, CurrentUserService u) { repo = r; users = u; }

    @GetMapping
    public List<Budget> list(Authentication a) {
        return repo.findByUserIdOrderByCategory(users.requireUser(a).getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Budget create(Authentication a, @Valid @RequestBody BudgetRequest r) {
        User u = users.requireUser(a);
        return repo.save(new Budget(u, r.category().trim(), r.monthlyLimit()));
    }

    @PutMapping("/{id}")
    public Budget update(Authentication a, @PathVariable Integer id, @Valid @RequestBody BudgetRequest r) {
        User u = users.requireUser(a);
        Budget budget = repo.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        budget.update(r.category().trim(), r.monthlyLimit());
        return repo.save(budget);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication a, @PathVariable Integer id) {
        User u = users.requireUser(a);
        Budget budget = repo.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        repo.delete(budget);
    }
}