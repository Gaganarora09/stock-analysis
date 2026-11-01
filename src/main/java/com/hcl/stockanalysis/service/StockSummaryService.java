package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.StockSummaryDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockSummaryService {

    private static final Logger log = LoggerFactory.getLogger(StockSummaryService.class);
    private static final long TTL_MILLIS = 60_000L; // 60 seconds in-memory TTL

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final AnalysisService analysisService;

    private volatile CachedSummary cachedSummary;

    private static record CachedSummary(List<StockSummaryDto> data, long timestamp) {
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > TTL_MILLIS;
        }
    }

    public StockSummaryService(StockRepository stockRepository,
                               StockPriceRepository stockPriceRepository,
                               AnalysisService analysisService) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.analysisService = analysisService;
    }

    @Transactional(readOnly = true)
    public List<StockSummaryDto> getStockSummary() {
        CachedSummary current = this.cachedSummary;
        if (current != null && !current.isExpired()) {
            return current.data();
        }

        synchronized (this) {
            current = this.cachedSummary;
            if (current != null && !current.isExpired()) {
                return current.data();
            }

            log.info("Computing stock summaries (cache miss or expired)...");
            List<StockSummaryDto> summaryList = computeStockSummary();
            this.cachedSummary = new CachedSummary(summaryList, System.currentTimeMillis());
            return summaryList;
        }
    }

    public void evictCache() {
        log.info("Evicting stock summary cache due to stock or price updates.");
        this.cachedSummary = null;
    }

    private List<StockSummaryDto> computeStockSummary() {
        // Query 1: Retrieve all configured stocks
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return Collections.emptyList();
        }

        // Query 2: Retrieve all historical prices with stock eagerly loaded in a single query
        List<StockPrice> allPrices = stockPriceRepository.findAllPricesWithStockAsc();

        // Group prices by Stock ID in memory without any additional database queries
        Map<Long, List<StockPrice>> pricesByStockId = allPrices.stream()
                .collect(Collectors.groupingBy(sp -> sp.getStock().getId()));

        List<StockSummaryDto> result = new ArrayList<>(stocks.size());
        for (Stock stock : stocks) {
            List<StockPrice> stockPrices = pricesByStockId.getOrDefault(stock.getId(), Collections.emptyList());
            result.add(analysisService.summarizeStock(stock, stockPrices));
        }

        return result;
    }
}
