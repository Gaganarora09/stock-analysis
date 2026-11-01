package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.CreateStockRequest;
import com.hcl.stockanalysis.dto.StockDto;
import com.hcl.stockanalysis.dto.UserSummaryDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.exception.StockAlreadyExistsException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import com.hcl.stockanalysis.repository.UserRepository;
import com.hcl.stockanalysis.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockSummaryService stockSummaryService;

    public AdminService(StockRepository stockRepository,
                        StockPriceRepository stockPriceRepository,
                        WatchlistRepository watchlistRepository,
                        UserRepository userRepository,
                        StockSummaryService stockSummaryService) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.stockSummaryService = stockSummaryService;
    }

    @Transactional
    public StockDto createStock(CreateStockRequest request) {
        String rawSymbol = request.getSymbol();
        if (rawSymbol == null || !rawSymbol.matches("^[A-Z0-9.-]{1,15}$")) {
            throw new IllegalArgumentException("Symbol must contain letters, digits, dot and hyphen only, up to 15 characters, converted to uppercase (no spaces or lowercase letters)");
        }
        String symbol = rawSymbol.trim();
        if (stockRepository.existsBySymbol(symbol)) {
            throw new StockAlreadyExistsException("Stock with symbol already exists: " + symbol);
        }

        Stock stock = Stock.builder()
                .symbol(symbol)
                .companyName(request.getName().trim())
                .sector(request.getSector().trim())
                .dataSource("SAMPLE")
                .lastUpdated(java.time.LocalDateTime.now())
                .build();
        Stock savedStock = stockRepository.save(stock);

        // Generate 60 days of continuous realistic prices (random walk around startingPrice)
        List<StockPrice> prices = generatePrices(savedStock, request.getStartingPrice(), 60);
        stockPriceRepository.saveAll(prices);

        stockSummaryService.evictCache();

        return new StockDto(savedStock.getId(), savedStock.getSymbol(), savedStock.getCompanyName(), savedStock.getSector(), savedStock.getDataSource(), savedStock.getLastUpdated());
    }

    @Transactional
    public void deleteStock(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));

        // Delete watchlist entries first to prevent FK constraint violations
        watchlistRepository.deleteByStock(stock);

        // Delete prices
        stockPriceRepository.deleteByStock(stock);

        // Delete stock
        stockRepository.delete(stock);

        stockSummaryService.evictCache();
    }

    public List<UserSummaryDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDto(u.getUsername(), u.getRole(), u.getEmail()))
                .collect(Collectors.toList());
    }

    private List<StockPrice> generatePrices(Stock stock, BigDecimal startingPrice, int days) {
        List<StockPrice> prices = new ArrayList<>(days);
        Random random = new Random(stock.getSymbol().hashCode()); // Deterministic pseudo-random seed per symbol
        BigDecimal currentClose = startingPrice;
        LocalDate startDate = LocalDate.now().minusDays(days - 1);

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            BigDecimal open = currentClose;

            // Random daily return between -2.0% and +2.0%
            double changeRatio = (random.nextDouble() - 0.49) * 0.04;
            BigDecimal close = open.multiply(BigDecimal.valueOf(1.0 + changeRatio)).setScale(2, RoundingMode.HALF_UP);
            if (close.compareTo(BigDecimal.valueOf(1.0)) < 0) {
                close = BigDecimal.valueOf(1.0);
            }

            BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1.0 + random.nextDouble() * 0.015)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(1.0 - random.nextDouble() * 0.015)).setScale(2, RoundingMode.HALF_UP);
            if (low.compareTo(BigDecimal.valueOf(0.5)) < 0) {
                low = BigDecimal.valueOf(0.5);
            }

            long volume = 500_000L + (long) (random.nextDouble() * 2_000_000L);

            StockPrice sp = new StockPrice();
            sp.setStock(stock);
            sp.setPriceDate(date);
            sp.setOpenPrice(open);
            sp.setClosePrice(close);
            sp.setHighPrice(high);
            sp.setLowPrice(low);
            sp.setVolume(volume);

            prices.add(sp);
            currentClose = close;
        }

        return prices;
    }
}
