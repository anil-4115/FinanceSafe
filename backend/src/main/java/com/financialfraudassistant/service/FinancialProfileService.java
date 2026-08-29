package com.financialfraudassistant.service;
import com.financialfraudassistant.dto.FinancialProfileRequest;
import com.financialfraudassistant.model.FinancialProfile;
import com.financialfraudassistant.model.User;
import com.financialfraudassistant.repository.FinancialProfileRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
public class FinancialProfileService {
    private final FinancialProfileRepository repository;
    public FinancialProfileService(FinancialProfileRepository repository) { this.repository = repository; }
    public FinancialProfile get(User user) { return repository.findByUserId(user.getId()).orElse(null); }
    public FinancialProfile save(User user, FinancialProfileRequest request) {
        FinancialProfile profile = repository.findByUserId(user.getId()).orElseGet(() -> new FinancialProfile(user));
        profile.update(request.ageRange(), request.employmentType(), zeroIfNull(request.monthlyIncome()),
                zeroIfNull(request.monthlyFixedExpenses()), zeroIfNull(request.savings()), zeroIfNull(request.existingInvestments()),
                zeroIfNull(request.debt()), request.riskTolerance(), request.investmentExperience(), request.preferredCategories());
        return repository.save(profile);
    }
    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
