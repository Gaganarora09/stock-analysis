package com.hcl.stockanalysis.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.stockanalysis.config.DatabaseSequenceSynchronizer;
import com.hcl.stockanalysis.dto.CreateStockRequest;
import com.hcl.stockanalysis.dto.RegisterRequest;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import com.hcl.stockanalysis.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceSyncIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseSequenceSynchronizer sequenceSynchronizer;

    @Test
    @Order(1)
    @DisplayName("(a) After seed data is loaded, adding a stock through the admin endpoint succeeds and creates 60 price rows")
    void testA_addStockAfterSeeding_creates60Prices() throws Exception {
        CreateStockRequest req = new CreateStockRequest("COALINDIA", "Coal India Ltd", "Energy", BigDecimal.valueOf(450.0));

        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("COALINDIA"))
                .andExpect(jsonPath("$.id").exists());

        Stock saved = stockRepository.findBySymbol("COALINDIA").orElseThrow();
        assertThat(saved.getId()).isNotNull();
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("COALINDIA")).hasSize(60);
    }

    @Test
    @Order(2)
    @DisplayName("(b) Adding two more stocks in a row succeeds with consecutive non-colliding IDs")
    void testB_addTwoMoreStocksInARow_succeeds() throws Exception {
        CreateStockRequest req1 = new CreateStockRequest("ONGC", "Oil and Natural Gas Corporation", "Energy", BigDecimal.valueOf(280.0));
        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("ONGC"));

        CreateStockRequest req2 = new CreateStockRequest("NTPC", "NTPC Ltd", "Utilities", BigDecimal.valueOf(390.0));
        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("NTPC"));

        Stock ongc = stockRepository.findBySymbol("ONGC").orElseThrow();
        Stock ntpc = stockRepository.findBySymbol("NTPC").orElseThrow();

        assertThat(ongc.getId()).isNotNull();
        assertThat(ntpc.getId()).isNotNull();
        assertThat(ntpc.getId()).isGreaterThan(ongc.getId());
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("ONGC")).hasSize(60);
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("NTPC")).hasSize(60);
    }

    @Test
    @Order(3)
    @DisplayName("(c) Registering a new user succeeds without primary key violation")
    void testC_registerNewUser_succeeds() throws Exception {
        RegisterRequest req = new RegisterRequest("new_trader_99", "Pass1234!", "newtrader99@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_trader_99"));

        User user = userRepository.findByUsername("new_trader_99").orElseThrow();
        assertThat(user.getId()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("(d) Counter fix when run twice changes nothing and subsequent inserts continue to succeed")
    void testD_counterFixRunTwice_changesNothing() throws Exception {
        // Run sequence synchronizer once
        sequenceSynchronizer.syncDatabaseSequences();

        // Run sequence synchronizer a second time
        sequenceSynchronizer.syncDatabaseSequences();

        // Verify that after running twice, inserting another stock succeeds smoothly
        CreateStockRequest req = new CreateStockRequest("POWERGRID", "Power Grid Corporation", "Utilities", BigDecimal.valueOf(320.0));
        mockMvc.perform(post("/api/admin/stocks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("POWERGRID"))
                .andExpect(jsonPath("$.id").exists());

        Stock powergrid = stockRepository.findBySymbol("POWERGRID").orElseThrow();
        assertThat(powergrid.getId()).isNotNull();
        assertThat(stockPriceRepository.findByStockSymbolOrderByPriceDateDesc("POWERGRID")).hasSize(60);
    }
}
