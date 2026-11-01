package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.StockSummaryDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSummaryServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockPriceRepository stockPriceRepository;

    private AnalysisService analysisService;
    private StockSummaryService stockSummaryService;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(stockRepository, stockPriceRepository);
        stockSummaryService = new StockSummaryService(stockRepository, stockPriceRepository, analysisService);

        testStock = Stock.builder()
                .id(1L)
                .symbol("TCS")
                .companyName("Tata Consultancy Services")
                .sector("IT")
                .dataSource("SAMPLE")
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("StockSummary computes indicators and signals in single batch")
    void getStockSummary_returnsAllStocksWithComputedSignals() {
        when(stockRepository.findAll()).thenReturn(List.of(testStock));

        List<StockPrice> prices = createPriceSeries(testStock, List.of(
                100.0, 101.0, 102.0, 103.0, 104.0, 105.0, 106.0, 107.0, 108.0, 109.0,
                110.0, 111.0, 112.0, 113.0, 114.0, 115.0, 116.0, 117.0, 118.0, 120.0
        ));
        when(stockPriceRepository.findAllPricesWithStockAsc()).thenReturn(prices);

        List<StockSummaryDto> summaries = stockSummaryService.getStockSummary();

        assertThat(summaries).hasSize(1);
        StockSummaryDto s = summaries.get(0);
        assertThat(s.getSymbol()).isEqualTo("TCS");
        assertThat(s.getCompanyName()).isEqualTo("Tata Consultancy Services");
        assertThat(s.getLatestPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(s.getSignal()).isIn("BUY", "HOLD", "SELL");
        assertThat(s.getSignalReason()).isNotEmpty();
    }

    @Test
    @DisplayName("StockSummary caches results for subsequent calls within TTL")
    void getStockSummary_cachesResultWithinTTL() {
        when(stockRepository.findAll()).thenReturn(List.of(testStock));
        when(stockPriceRepository.findAllPricesWithStockAsc()).thenReturn(List.of());

        // First call populates cache
        stockSummaryService.getStockSummary();
        // Second call should serve from cache
        stockSummaryService.getStockSummary();

        // Repositories should only be invoked once
        verify(stockRepository, times(1)).findAll();
        verify(stockPriceRepository, times(1)).findAllPricesWithStockAsc();
    }

    @Test
    @DisplayName("evictCache clears cached results so next call re-queries repositories")
    void evictCache_clearsCacheAndTriggersNewQuery() {
        when(stockRepository.findAll()).thenReturn(List.of(testStock));
        when(stockPriceRepository.findAllPricesWithStockAsc()).thenReturn(List.of());

        stockSummaryService.getStockSummary();
        stockSummaryService.evictCache();
        stockSummaryService.getStockSummary();

        // Repositories should be invoked twice because cache was invalidated
        verify(stockRepository, times(2)).findAll();
        verify(stockPriceRepository, times(2)).findAllPricesWithStockAsc();
    }

    @Test
    @DisplayName("Empty stocks list returns empty summary list without errors")
    void getStockSummary_emptyStocks_returnsEmptyList() {
        when(stockRepository.findAll()).thenReturn(List.of());

        List<StockSummaryDto> summaries = stockSummaryService.getStockSummary();

        assertThat(summaries).isEmpty();
        verify(stockPriceRepository, never()).findAllPricesWithStockAsc();
    }

    private List<StockPrice> createPriceSeries(Stock stock, List<Double> closes) {
        List<StockPrice> list = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal close = BigDecimal.valueOf(closes.get(i)).setScale(2, RoundingMode.HALF_UP);
            StockPrice sp = new StockPrice();
            sp.setId((long) (i + 1));
            sp.setStock(stock);
            sp.setPriceDate(startDate.plusDays(i));
            sp.setClosePrice(close);
            sp.setOpenPrice(close);
            sp.setHighPrice(close.add(BigDecimal.ONE));
            sp.setLowPrice(close.subtract(BigDecimal.ONE));
            sp.setVolume(100000L);
            list.add(sp);
        }
        return list;
    }
}
