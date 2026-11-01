package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.DailyCloseDto;
import com.hcl.stockanalysis.dto.MarketRefreshResultDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.RateLimitExceededException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataClient marketDataClient;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    private final boolean enabled;
    private final String apiKey;

    private final Map<String, LocalDate> lastRefreshPerStock = new ConcurrentHashMap<>();
    private volatile LocalDate rateLimitDate = null;

    public MarketDataService(
            MarketDataClient marketDataClient,
            StockRepository stockRepository,
            StockPriceRepository stockPriceRepository,
            @Value("${app.marketdata.enabled:false}") boolean enabled,
            @Value("${app.marketdata.api-key:}") String apiKey) {
        this.marketDataClient = marketDataClient;
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public MarketRefreshResultDto refreshAllStocks(boolean force) {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return new MarketRefreshResultDto(0, 0, 0, "No stocks configured in database.");
        }

        if (!enabled || apiKey.isEmpty()) {
            log.info("Market data sync skipped: app.marketdata.enabled=false or API key is not configured.");
            return new MarketRefreshResultDto(0, stocks.size(), 0, "Live market data disabled or API key unset. Retaining fallback data.");
        }

        LocalDate today = LocalDate.now();
        if (rateLimitDate != null && rateLimitDate.equals(today)) {
            log.warn("Market data sync skipped: Alpha Vantage daily request limit reached earlier today.");
            return new MarketRefreshResultDto(0, stocks.size(), 0, "Daily API quota reached earlier today. Retaining existing prices.");
        }

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Stock stock : stocks) {
            String symbol = stock.getSymbol();

            // Refresh at most once per day per stock unless force requested
            if (!force && today.equals(lastRefreshPerStock.get(symbol))) {
                skipped++;
                continue;
            }

            try {
                String lookupSymbol = (stock.getMarketSymbol() != null && !stock.getMarketSymbol().isBlank())
                        ? stock.getMarketSymbol()
                        : stock.getSymbol();

                List<DailyCloseDto> livePrices = marketDataClient.fetchDailyCloses(lookupSymbol);

                if (livePrices == null || livePrices.isEmpty()) {
                    log.info("No live prices returned for symbol: {}. Retaining existing data.", symbol);
                    skipped++;
                    continue;
                }

                upsertStockPrices(stock, livePrices);
                lastRefreshPerStock.put(symbol, today);
                updated++;
                log.info("Successfully updated market data for {}: {} dates processed.", symbol, livePrices.size());

            } catch (RateLimitExceededException rle) {
                log.warn("Alpha Vantage rate limit reached on symbol {}. Halting updates for the day.", symbol);
                this.rateLimitDate = today;
                skipped++;
                break; // Stop for the day after the first rate-limit response
            } catch (Exception ex) {
                log.warn("Failed to update market data for {}: {}. Retaining existing prices.", symbol, ex.getMessage());
                failed++;
            }
        }

        String summary = String.format("Market sync finished: %d updated, %d skipped, %d failed.", updated, skipped, failed);
        log.info(summary);
        return new MarketRefreshResultDto(updated, skipped, failed, summary);
    }

    @Transactional
    public void upsertStockPrices(Stock stock, List<DailyCloseDto> prices) {
        for (DailyCloseDto p : prices) {
            Optional<StockPrice> existing = stockPriceRepository.findByStockAndPriceDate(stock, p.date());
            if (existing.isPresent()) {
                StockPrice sp = existing.get();
                sp.setClosePrice(p.close());
                if (p.open() != null) sp.setOpenPrice(p.open());
                if (p.high() != null) sp.setHighPrice(p.high());
                if (p.low() != null) sp.setLowPrice(p.low());
                if (p.volume() != null) sp.setVolume(p.volume());
                stockPriceRepository.save(sp);
            } else {
                StockPrice sp = new StockPrice();
                sp.setStock(stock);
                sp.setPriceDate(p.date());
                sp.setClosePrice(p.close());
                sp.setOpenPrice(p.open() != null ? p.open() : p.close());
                sp.setHighPrice(p.high() != null ? p.high() : p.close());
                sp.setLowPrice(p.low() != null ? p.low() : p.close());
                sp.setVolume(p.volume() != null ? p.volume() : 1_000_000L);
                stockPriceRepository.save(sp);
            }
        }

        stock.setDataSource("LIVE");
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);
    }

    @Scheduled(cron = "${app.marketdata.cron:0 0 18 * * MON-FRI}")
    public void scheduledDailyRefresh() {
        if (enabled && !apiKey.isEmpty()) {
            log.info("Executing scheduled daily market data refresh...");
            refreshAllStocks(false);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!enabled || apiKey.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Short delay to allow application context to settle
                Thread.sleep(3000);
                boolean needsRefresh = stockRepository.findAll().stream()
                        .anyMatch(s -> s.getLastUpdated() == null || s.getLastUpdated().isBefore(LocalDateTime.now().minusDays(1)));

                if (needsRefresh) {
                    log.info("Startup market data check: data is older than 1 day. Refreshing in background...");
                    refreshAllStocks(false);
                }
            } catch (Exception e) {
                log.warn("Background startup market refresh error: {}", e.getMessage());
            }
        });
    }
}
