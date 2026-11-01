package com.hcl.stockanalysis.controller;

import com.hcl.stockanalysis.dto.WatchlistItemDto;
import com.hcl.stockanalysis.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist", description = "User watchlist management endpoints")
@SecurityRequirement(name = "basicAuth")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    @Operation(summary = "Get current user's watchlist")
    public ResponseEntity<List<WatchlistItemDto>> getWatchlist(Authentication authentication) {
        return ResponseEntity.ok(watchlistService.getWatchlist(authentication.getName()));
    }

    @PostMapping("/{symbol}")
    @Operation(summary = "Add a stock to watchlist (idempotent)")
    public ResponseEntity<Void> addToWatchlist(
            @Parameter(description = "Stock symbol e.g. HCLTECH")
            @PathVariable String symbol,
            Authentication authentication) {
        watchlistService.addToWatchlist(authentication.getName(), symbol);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{symbol}")
    @Operation(summary = "Remove a stock from watchlist")
    public ResponseEntity<Void> removeFromWatchlist(
            @Parameter(description = "Stock symbol e.g. HCLTECH")
            @PathVariable String symbol,
            Authentication authentication) {
        watchlistService.removeFromWatchlist(authentication.getName(), symbol);
        return ResponseEntity.noContent().build();
    }
}
