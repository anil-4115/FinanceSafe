package com.financialfraudassistant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String email() {
        return "sec-it-" + UUID.randomUUID() + "@example.com";
    }

    private String register(String userName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", userName, "email", email(), "password", "Passw0rd!123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private int createTransaction(String token, String type) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionDate", "2026-08-01", "merchant", "Test Merchant " + type,
                                "amount", 1500, "transactionType", "INCOME", "category", "Salary"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private int analyzeScam(String token, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/fraud/analyze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_returnsOk() throws Exception {
        String token = register("Valid Token User");
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoints_areReachableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void crossUser_transactionWrite_isForbiddenForOtherUser() throws Exception {
        String owner = register("Owner");
        String intruder = register("Intruder");
        int transactionId = createTransaction(owner, "Owner");

        mockMvc.perform(delete("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionDate", "2026-08-02", "merchant", "Hacked",
                                "amount", 100, "transactionType", "EXPENSE", "category", "Other"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossUser_fraudAnalysis_isNotVisibleToOtherUser() throws Exception {
        String owner = register("Owner");
        String intruder = register("Intruder");
        int analysisId = analyzeScam(owner, "Your account will be suspended, share OTP now");

        mockMvc.perform(get("/api/fraud/history/{id}", analysisId)
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/fraud/history")
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crossUser_alertResolution_failsForOtherUser() throws Exception {
        String owner = register("Owner");
        String intruder = register("Intruder");
        mockMvc.perform(post("/api/fraud/reports")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "channel", "SMS", "description", "KYC update scam asking for OTP", "amountAtRisk", 0))))
                .andExpect(status().isCreated());

        MvcResult alertsResult = mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode alerts = objectMapper.readTree(alertsResult.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(alerts).isNotEmpty();
        int alertId = alerts.get(0).get("id").asInt();

        mockMvc.perform(patch("/api/alerts/{id}/resolve", alertId)
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidIds_returnNotFound() throws Exception {
        String token = register("User");
        mockMvc.perform(get("/api/fraud/history/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/transactions/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void credentialData_neverAppearsInApiResponses() throws Exception {
        String token = register("Credential Guard");
        String auth = "Bearer " + token;

        mockMvc.perform(post("/api/budgets").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("category", "Food", "monthlyLimit", 4000))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/profile").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ageRange", "18-25", "employmentType", "Student", "monthlyIncome", 40000,
                                "monthlyFixedExpenses", 8000, "savings", 10000, "existingInvestments", 0,
                                "debt", 0, "riskTolerance", "Moderate", "investmentExperience", "Beginner",
                                "preferredCategories", "Mutual funds"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/goals").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Vacation", "targetAmount", 20000, "currentAmount", 5000,
                                "deadline", "2027-06-01", "monthlyContribution", 2000))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));

        mockMvc.perform(get("/api/budgets").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));

        mockMvc.perform(get("/api/goals").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));

        mockMvc.perform(get("/api/profile").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("passwordHash"))));
    }

    @Test
    void incidentsFeed_requiresAuthentication_andStripsUserIdentity() throws Exception {
        mockMvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());

        String token = register("Incident Viewer");
        String auth = "Bearer " + token;
        mockMvc.perform(post("/api/fraud/reports").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "channel", "SMS", "description", "Fake bank security alert scam", "amountAtRisk", 0))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/incidents").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("passwordHash");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("@example.com");
    }
}