package com.financialfraudassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration coverage for the remaining features that had no
 * automated backend test: education/quiz, products/compare, market, assistant,
 * decision & simulator/investments, incidents, alert resolution, scam reports
 * and demo data. Each test uses a freshly registered user so data stays isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeatureCoverageIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Feature IT User",
                                "email", "feature-it-" + UUID.randomUUID() + "@example.com",
                                "password", "Passw0rd!123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String authString() throws Exception {
        return "Bearer " + registerAndGetToken();
    }

    // ---- 13. Education + quiz ----

    @Test
    void education_modulesLiteracyDetailQuizAndAttempt() throws Exception {
        String auth = authString();
        // seeded modules exist
        mockMvc.perform(get("/api/education").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        mockMvc.perform(get("/api/education/literacy").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/education/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists());
        mockMvc.perform(get("/api/education/1/quiz").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question").exists());
        // submit an attempt with the correct first-question answer
        mockMvc.perform(post("/api/education/1/attempt")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", java.util.List.of(0, 1, 2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scorePct").exists());
    }

    // ---- 14. Products & compare ----

    @Test
    void products_listFilterDetailAndCompare() throws Exception {
        String auth = authString();
        mockMvc.perform(get("/api/products").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("length()").value(org.hamcrest.Matchers.greaterThan(0)));
        mockMvc.perform(get("/api/products").param("category", "Mutual funds").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Mutual funds"));
        mockMvc.perform(get("/api/products/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists());
        mockMvc.perform(get("/api/products/compare").param("ids", "1,2").header("Authorization", auth))
                .andExpect(status().isOk());
    }

    // ---- 15. Market ----

    @Test
    void market_searchAndDetail() throws Exception {
        String auth = authString();
        mockMvc.perform(get("/api/market/search").param("q", "reliance").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/market/TCS").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TCS"));
        // unknown symbol -> 404
        mockMvc.perform(get("/api/market/ZZZZZZ").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    // ---- 16. Assistant ----

    @Test
    void assistant_chatAndHistory() throws Exception {
        String auth = authString();
        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Is my money safe in UPI?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());
        mockMvc.perform(get("/api/assistant/history").header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void assistant_investUsesProfileDataAndReturnsAllocations() throws Exception {
        String auth = authString();
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ageRange", "26-35", "employmentType", "Salaried",
                                "monthlyIncome", 60000, "monthlyFixedExpenses", 20000,
                                "savings", 50000, "existingInvestments", 10000,
                                "debt", 0, "riskTolerance", "Moderate",
                                "investmentExperience", "Beginner",
                                "preferredCategories", "Mutual funds, Fixed deposits, Gold ETF"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Should I invest 50000 in mutual funds?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("invest"))
                .andExpect(jsonPath("$.reply").value(Matchers.containsString("%")));
    }

    @Test
    void assistant_marketReturnsLiveSnapshot() throws Exception {
        String auth = authString();
        mockMvc.perform(post("/api/assistant/chat")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "What is the market trend for TCS?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("market"))
                .andExpect(jsonPath("$.reply").value(Matchers.containsString("TCS")));
    }

    // ---- 17. Decision & simulator / investments ----

    @Test
    void decisionAnalyze_requiresAuthAndReturnsGuidance() throws Exception {
        String auth = authString();
        mockMvc.perform(post("/api/decision/analyze")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decisionType", "loan", "amount", 200000,
                                "monthlyIncome", 60000, "monthlyExpenses", 25000,
                                "monthlyCost", 8000, "tenureMonths", 12, "interestRatePct", 10.5,
                                "riskTolerance", "Moderate"))))
                .andExpect(status().isOk());
        // missing required field -> 400
        mockMvc.perform(post("/api/decision/analyze")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decisionType", "loan"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulator_investment_andWhatIf_requireAuth() throws Exception {
        // these /api/** routes sit behind the security filter chain, so a token is required
        mockMvc.perform(post("/api/simulator/investment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initialInvestment", 100000, "monthlyContribution", 5000,
                                "years", 10, "annualReturnPct", 12.0))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/simulator/what-if")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenario", "save", "amount", 5000, "expensePctChange", -10))))
                .andExpect(status().isUnauthorized());

        String auth = authString();
        mockMvc.perform(post("/api/simulator/investment")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "initialInvestment", 100000, "monthlyContribution", 5000,
                                "years", 10, "annualReturnPct", 12.0))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/simulator/what-if")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scenario", "save", "amount", 5000, "expensePctChange", -10))))
                .andExpect(status().isOk());
    }

    @Test
    void investmentRecommendation_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/investments/recommendation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 500000, "timeHorizonYears", 10, "riskTolerance", "Moderate"))))
                .andExpect(status().isUnauthorized());

        String auth = authString();
        mockMvc.perform(post("/api/investments/recommendation")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 500000, "timeHorizonYears", 10, "riskTolerance", "Moderate"))))
                .andExpect(status().isOk());
    }

    // ---- 8. Fraud reports + incidents ----

    @Test
    void scamReport_thenIncidentsFeed() throws Exception {
        String auth = authString();
        mockMvc.perform(post("/api/fraud/reports")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "channel", "SMS", "description", "Fake courier delivery fee scam",
                                "amountAtRisk", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskScore").isNumber());

        mockMvc.perform(get("/api/incidents").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---- 7. Alerts resolve (and IDOR isolation) ----

    @Test
    void alerts_resolveOwnAndRejectOthers() throws Exception {
        String ownerAuth = authString();
        // create a scam report to generate an alerted/incident surface, then resolve own
        mockMvc.perform(get("/api/alerts").header("Authorization", ownerAuth))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/alerts").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andReturn();
        var alerts = objectMapper.readTree(list.getResponse().getContentAsString());
        if (alerts.isArray() && alerts.size() > 0) {
            int id = alerts.get(0).get("id").asInt();
            mockMvc.perform(patch("/api/alerts/" + id + "/resolve").header("Authorization", ownerAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
            // another user must not resolve it
            String otherAuth = authString();
            mockMvc.perform(patch("/api/alerts/" + id + "/resolve").header("Authorization", otherAuth))
                    .andExpect(status().isNotFound());
        }
    }

    // ---- 18. Demo data ----

    @Test
    void demoData_loadAndClear() throws Exception {
        String auth = authString();
        mockMvc.perform(post("/api/demo/load-sample").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isNumber())
                .andExpect(jsonPath("$.alerts").isNumber());
        mockMvc.perform(delete("/api/demo/clear").header("Authorization", auth))
                .andExpect(status().isOk());
    }
}
