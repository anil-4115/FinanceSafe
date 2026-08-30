package com.financialfraudassistant.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Finance IT User",
                                "email", "finance-it-" + UUID.randomUUID() + "@example.com",
                                "password", "Passw0rd!123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void fullFinancialFlow_profileTransactionsBudgetGoalAndHealthScore() throws Exception {
        String token = registerAndGetToken();
        String auth = "Bearer " + token;

        mockMvc.perform(put("/api/profile")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ageRange", "18-25", "employmentType", "Student",
                                "monthlyIncome", 50000, "monthlyFixedExpenses", 12000,
                                "savings", 60000, "existingInvestments", 20000,
                                "debt", 0, "riskTolerance", "Moderate", "investmentExperience", "Beginner",
                                "preferredCategories", "Mutual funds, Fixed deposits, Gold ETF"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionDate", "2026-08-01", "merchant", "Salary", "amount", 50000,
                                "transactionType", "INCOME", "category", "Salary"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionDate", "2026-08-02", "merchant", "Grocery", "amount", 1450,
                                "transactionType", "EXPENSE", "category", "Food"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("category", "Food", "monthlyLimit", 4000))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/goals")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Emergency fund", "targetAmount", 100000, "currentAmount", 60000,
                                "deadline", "2027-01-01", "monthlyContribution", 5000))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/health-score").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.components.length()").value(8));

        mockMvc.perform(get("/api/dashboard").header("Authorization", auth))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTransactionAmount_returnsBadRequest() throws Exception {
        String token = registerAndGetToken();
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionDate", "2026-08-01", "merchant", "Bad",
                                "amount", -50, "transactionType", "EXPENSE", "category", "Other"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthScore_withoutProfile_stillReturnsAValue() throws Exception {
        String token = registerAndGetToken();
        mockMvc.perform(get("/api/health-score").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber());
    }

    @Test
    void csvImport_withoutMerchantColumn_importsRowsAsUnknown() throws Exception {
        String token = registerAndGetToken();
        String csv = "date,amount,type,category\n2026-08-05,1200,expense,Food\n2026-08-06,2500,expense,Travel\n";
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", csv.getBytes());
        mockMvc.perform(multipart("/api/transactions/import").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant").value("Unknown"));
    }

    @Test
    void csvImport_withNarrationSynonym_usesNarrationAsMerchant() throws Exception {
        String token = registerAndGetToken();
        String csv = "date,narration,amount,category\n2026-08-07,Swiggy,460,Food\n";
        MockMultipartFile file = new MockMultipartFile("file", "statement.csv", "text/csv", csv.getBytes());
        mockMvc.perform(multipart("/api/transactions/import").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchant").value("Swiggy"));
    }
}