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
}