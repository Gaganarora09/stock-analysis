package com.hcl.stockanalysis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.stockanalysis.dto.CreateStockRequest;
import com.hcl.stockanalysis.dto.RegisterRequest;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.entity.Watchlist;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import com.hcl.stockanalysis.repository.UserRepository;
import com.hcl.stockanalysis.repository.WatchlistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    @DisplayName("Admin accessing /api/admin/users gets 200 OK")
    void adminUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Normal user (alice) accessing /api/admin/users gets 403 Forbidden")
    void adminUsers_asNormalUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(httpBasic("alice", "alice123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/admin/users returns 401 Unauthorized")
    void adminUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registration cannot create an admin (always assigns ROLE_USER)")
    void register_cannotCreateAdmin() throws Exception {
        RegisterRequest req = new RegisterRequest("sneaky_admin_attempt", "Password123!", "sneaky@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        User saved = userRepository.findByUsername("sneaky_admin_attempt").orElseThrow();
        assertThat(saved.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Admin POST /api/admin/stocks creates stock and 60 price records")
    void adminStocks_addStock_creates60PriceRows() throws Exception {
        CreateStockRequest req = new CreateStockRequest("BEL", "Bharat Electronics Ltd", "Defense", BigDecimal.valueOf(290.0));

        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("BEL"));

        assertThat(stockRepository.findBySymbol("BEL")).isPresent();
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("BEL")).hasSize(60);
    }

    @Test
    @DisplayName("Admin POST /api/admin/stocks rejects duplicate symbol with 409 Conflict")
    void adminStocks_duplicateSymbol_isRejected() throws Exception {
        // HCLTECH already seeded in data.sql
        CreateStockRequest req = new CreateStockRequest("HCLTECH", "Duplicate HCL", "IT", BigDecimal.valueOf(1200.0));

        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Admin DELETE /api/admin/stocks/{symbol} deletes stock along with its prices and watchlist entries")
    void adminStocks_deleteStock_removesPricesAndWatchlist() throws Exception {
        // Create stock DELCORP
        Stock stock = stockRepository.save(Stock.builder()
                .symbol("DELCORP")
                .companyName("Delete Corp")
                .sector("Industrial")
                .build());

        // Add a price
        com.hcl.stockanalysis.entity.StockPrice sp = new com.hcl.stockanalysis.entity.StockPrice();
        sp.setStock(stock);
        sp.setPriceDate(java.time.LocalDate.now());
        sp.setOpenPrice(BigDecimal.valueOf(100));
        sp.setClosePrice(BigDecimal.valueOf(102));
        sp.setHighPrice(BigDecimal.valueOf(103));
        sp.setLowPrice(BigDecimal.valueOf(99));
        sp.setVolume(1000L);
        stockPriceRepository.save(sp);

        // Add a watchlist entry
        User admin = userRepository.findByUsername("admin").orElseThrow();
        watchlistRepository.save(new Watchlist(admin, stock, LocalDateTime.now()));

        // Verify existing
        assertThat(stockRepository.findBySymbol("DELCORP")).isPresent();
        assertThat(watchlistRepository.existsByUserAndStock(admin, stock)).isTrue();

        // Perform delete as admin
        mockMvc.perform(delete("/api/admin/stocks/DELCORP")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        // Verify clean purge with no orphan records
        assertThat(stockRepository.findBySymbol("DELCORP")).isEmpty();
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("DELCORP")).isEmpty();
        assertThat(watchlistRepository.existsByUserAndStock(admin, stock)).isFalse();
    }
}
