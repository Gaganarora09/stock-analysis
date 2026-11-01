package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.DailyCloseDto;
import com.hcl.stockanalysis.dto.MarketRefreshResultDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.RateLimitExceededException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataClient marketDataClient;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockPriceRepository stockPriceRepository;

    private MarketDataService marketDataService;

    private Stock hclStock;
    private Stock infyStock;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataService(
                marketDataClient,
                stockRepository,
                stockPriceRepository,
                true,
                "TEST_API_KEY"
        );

        hclStock = Stock.builder()
                .id(1L)
                .symbol("HCLTECH")
                .companyName("HCL Technologies Ltd")
                .sector("IT")
                .dataSource("SAMPLE")
                .marketSymbol("HCLTECH.BSE")
                .build();

        infyStock = Stock.builder()
                .id(2L)
                .symbol("INFY")
                .companyName("Infosys Ltd")
                .sector("IT")
                .dataSource("SAMPLE")
                .marketSymbol("INFY.BSE")
                .build();
    }

    @Test
    @DisplayName("Upsert inserts new stock price row when date does not exist")
    void upsertStockPrices_newDate_insertsRow() {
        LocalDate date = LocalDate.of(2026, 10, 31);
        BigDecimal close = new BigDecimal("1550.00");
        DailyCloseDto dto = new DailyCloseDto(date, close, new BigDecimal("1540.00"), new BigDecimal("1560.00"), new BigDecimal("1535.00"), 500000L);

        when(stockPriceRepository.findByStockAndPriceDate(hclStock, date)).thenReturn(Optional.empty());

        marketDataService.upsertStockPrices(hclStock, List.of(dto));

        ArgumentCaptor<StockPrice> priceCaptor = ArgumentCaptor.forClass(StockPrice.class);
        verify(stockPriceRepository).save(priceCaptor.capture());
        StockPrice savedPrice = priceCaptor.getValue();

        assertThat(savedPrice.getStock()).isEqualTo(hclStock);
        assertThat(savedPrice.getPriceDate()).isEqualTo(date);
        assertThat(savedPrice.getClosePrice()).isEqualByComparingTo(close);

        verify(stockRepository).save(hclStock);
        assertThat(hclStock.getDataSource()).isEqualTo("LIVE");
        assertThat(hclStock.getLastUpdated()).isNotNull();
    }

    @Test
    @DisplayName("Upsert updates existing stock price without creating duplicate rows")
    void upsertStockPrices_existingDate_updatesExistingRow() {
        LocalDate date = LocalDate.of(2026, 10, 31);
        StockPrice existingPrice = new StockPrice();
        existingPrice.setId(101L);
        existingPrice.setStock(hclStock);
        existingPrice.setPriceDate(date);
        existingPrice.setClosePrice(new BigDecimal("1500.00"));

        when(stockPriceRepository.findByStockAndPriceDate(hclStock, date)).thenReturn(Optional.of(existingPrice));

        BigDecimal newClose = new BigDecimal("1585.50");
        DailyCloseDto dto = new DailyCloseDto(date, newClose, null, null, null, null);

        marketDataService.upsertStockPrices(hclStock, List.of(dto));

        ArgumentCaptor<StockPrice> priceCaptor = ArgumentCaptor.forClass(StockPrice.class);
        verify(stockPriceRepository).save(priceCaptor.capture());
        StockPrice savedPrice = priceCaptor.getValue();

        assertThat(savedPrice.getId()).isEqualTo(101L);
        assertThat(savedPrice.getClosePrice()).isEqualByComparingTo(newClose);
        assertThat(hclStock.getDataSource()).isEqualTo("LIVE");
    }

    @Test
    @DisplayName("Client exception keeps existing data safely and increments failed count")
    void refreshAllStocks_clientException_retainsExistingData() {
        when(stockRepository.findAll()).thenReturn(List.of(hclStock));
        when(marketDataClient.fetchDailyCloses("HCLTECH.BSE")).thenThrow(new RuntimeException("API connection timeout"));

        MarketRefreshResultDto result = marketDataService.refreshAllStocks(true);

        assertThat(result.getUpdated()).isEqualTo(0);
        assertThat(result.getFailed()).isEqualTo(1);
        verify(stockPriceRepository, never()).save(any());
        assertThat(hclStock.getDataSource()).isEqualTo("SAMPLE");
    }

    @Test
    @DisplayName("Rate limit response halts subsequent stock calls for the day")
    void refreshAllStocks_rateLimitHit_haltsFurtherCalls() {
        when(stockRepository.findAll()).thenReturn(List.of(hclStock, infyStock));
        when(marketDataClient.fetchDailyCloses("HCLTECH.BSE")).thenThrow(new RateLimitExceededException("Daily rate limit reached (25 req/day)"));

        MarketRefreshResultDto result = marketDataService.refreshAllStocks(true);

        assertThat(result.getUpdated()).isEqualTo(0);
        assertThat(result.getSkipped()).isGreaterThanOrEqualTo(1);
        // Verify INFY was never called because loop halted on rate limit
        verify(marketDataClient, never()).fetchDailyCloses("INFY.BSE");

        // Subsequent call on the same day skips immediately
        MarketRefreshResultDto secondResult = marketDataService.refreshAllStocks(true);
        assertThat(secondResult.getUpdated()).isEqualTo(0);
        assertThat(secondResult.getMessage()).contains("quota reached");
    }

    @Test
    @DisplayName("Disabled service or empty API key makes zero client calls")
    void refreshAllStocks_disabled_makesNoCalls() {
        MarketDataService disabledService = new MarketDataService(
                marketDataClient,
                stockRepository,
                stockPriceRepository,
                false,
                ""
        );

        when(stockRepository.findAll()).thenReturn(List.of(hclStock));

        MarketRefreshResultDto result = disabledService.refreshAllStocks(true);

        assertThat(result.getUpdated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getMessage()).contains("disabled");
        verifyNoInteractions(marketDataClient);
    }
}
