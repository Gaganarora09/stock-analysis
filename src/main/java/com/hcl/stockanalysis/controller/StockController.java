package com.hcl.stockanalysis.controller;

import com.hcl.stockanalysis.dto.AnalysisDto;
import com.hcl.stockanalysis.dto.PredictionDto;
import com.hcl.stockanalysis.dto.StockDto;
import com.hcl.stockanalysis.dto.StockPriceDto;
import com.hcl.stockanalysis.service.AnalysisService;
import com.hcl.stockanalysis.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "Stocks", description = "Stock data and analysis endpoints")
@SecurityRequirement(name = "basicAuth")
public class StockController {

    private final StockService stockService;
    private final AnalysisService analysisService;

    public StockController(StockService stockService, AnalysisService analysisService) {
        this.stockService = stockService;
        this.analysisService = analysisService;
    }

    @GetMapping
    @Operation(summary = "Get all stocks")
    public ResponseEntity<List<StockDto>> getAllStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get stock by symbol")
    public ResponseEntity<StockDto> getStockBySymbol(
            @Parameter(description = "Stock symbol e.g. HCLTECH, TCS, INFY")
            @PathVariable String symbol) {
        return ResponseEntity.ok(stockService.getStockBySymbol(symbol));
    }

    @GetMapping("/{symbol}/prices")
    @Operation(summary = "Get historical prices for a stock")
    public ResponseEntity<List<StockPriceDto>> getPrices(@PathVariable String symbol) {
        return ResponseEntity.ok(stockService.getPricesBySymbol(symbol));
    }

    @GetMapping("/{symbol}/analysis")
    @Operation(summary = "Get analysis with BUY/SELL/HOLD signal for a stock")
    public ResponseEntity<AnalysisDto> getAnalysis(@PathVariable String symbol) {
        return ResponseEntity.ok(analysisService.analyzeStock(symbol));
    }

    @GetMapping("/{symbol}/prediction")
    @Operation(summary = "Get 7-day price forecast using linear regression")
    public ResponseEntity<PredictionDto> getPrediction(@PathVariable String symbol) {
        return ResponseEntity.ok(analysisService.predictStock(symbol));
    }
}
