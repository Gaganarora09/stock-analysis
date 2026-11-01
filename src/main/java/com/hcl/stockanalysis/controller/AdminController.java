package com.hcl.stockanalysis.controller;

import com.hcl.stockanalysis.dto.CreateStockRequest;
import com.hcl.stockanalysis.dto.MarketRefreshResultDto;
import com.hcl.stockanalysis.dto.StockDto;
import com.hcl.stockanalysis.dto.UserSummaryDto;
import com.hcl.stockanalysis.service.AdminService;
import com.hcl.stockanalysis.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin stock and user management endpoints")
@SecurityRequirement(name = "basicAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final MarketDataService marketDataService;

    public AdminController(AdminService adminService, MarketDataService marketDataService) {
        this.adminService = adminService;
        this.marketDataService = marketDataService;
    }

    @PostMapping("/stocks")
    @Operation(summary = "Create a new stock and generate 60 days of historical prices (Admin only)")
    public ResponseEntity<StockDto> createStock(@Valid @RequestBody CreateStockRequest request) {
        StockDto stock = adminService.createStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    @DeleteMapping("/stocks/{symbol}")
    @Operation(summary = "Delete a stock, its prices, and all associated watchlist references (Admin only)")
    public ResponseEntity<Void> deleteStock(
            @Parameter(description = "Stock symbol e.g. HCLTECH")
            @PathVariable String symbol) {
        adminService.deleteStock(symbol);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Get list of all registered users with usernames and roles (Admin only)")
    public ResponseEntity<List<UserSummaryDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/market/refresh")
    @Operation(summary = "Manually trigger live market data refresh from Alpha Vantage (Admin only)")
    public ResponseEntity<MarketRefreshResultDto> refreshMarketData() {
        return ResponseEntity.ok(marketDataService.refreshAllStocks(true));
    }
}
