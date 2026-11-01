package com.hcl.stockanalysis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.stockanalysis.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthAndSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("/api/stocks without credentials returns 401 Unauthorized")
    void stocks_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/api/stocks with admin/admin123 returns 200 OK")
    void stocks_authenticatedAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/stocks").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/login.html is public and returns 200 OK")
    void loginHtml_public_returns200() throws Exception {
        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/register with valid data succeeds")
    void register_validData_succeeds() throws Exception {
        RegisterRequest validRequest = new RegisterRequest("new_trader_it", "Secret123!", "new_trader@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_trader_it"));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid data returns 400 Bad Request")
    void register_invalidData_returns400() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "", "not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    @DisplayName("/api/watchlist without credentials returns 401 Unauthorized")
    void watchlist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized());
    }
}
