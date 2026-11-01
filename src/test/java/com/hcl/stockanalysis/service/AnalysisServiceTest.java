package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.AnalysisDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockPriceRepository stockPriceRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        testStock = Stock.builder()
                .id(1L)
                .symbol("TEST")
                .companyName("Test Technologies Ltd")
                .sector("Information Technology")
                .build();
    }

    private StockPrice createPrice(LocalDate date, double open, double high, double low, double close, long volume) {
        StockPrice sp = new StockPrice();
        sp.setStock(testStock);
        sp.setPriceDate(date);
        sp.setOpenPrice(BigDecimal.valueOf(open));
        sp.setHighPrice(BigDecimal.valueOf(high));
        sp.setLowPrice(BigDecimal.valueOf(low));
        sp.setClosePrice(BigDecimal.valueOf(close));
        sp.setVolume(volume);
        return sp;
    }

    @Test
    @DisplayName("Fewer than 20 prices: computes moving averages from available days, neutral RSI(14)=50, and HOLD signal")
    void analyzeStock_tooLittleData_fewerThan20Prices() {
        when(stockRepository.findBySymbol("TEST")).thenReturn(Optional.of(testStock));

        // 3 days of prices
        List<StockPrice> pricesDesc = List.of(
                createPrice(LocalDate.of(2024, 1, 3), 115, 125, 105, 120, 1000),
                createPrice(LocalDate.of(2024, 1, 2), 105, 115, 98, 110, 1000),
                createPrice(LocalDate.of(2024, 1, 1), 95, 105, 95, 100, 1000)
        );
        when(stockPriceRepository.findLatestPricesBySymbol("TEST")).thenReturn(pricesDesc);

        AnalysisDto result = analysisService.analyzeStock("TEST");

        assertThat(result.getSymbol()).isEqualTo("TEST");
        assertThat(result.getLatestClosePrice()).isEqualByComparingTo("120.00");
        // (120 - 110) / 110 * 100 = 9.09%
        assertThat(result.getPercentageChange()).isEqualByComparingTo("9.09");
        // SMA 5 and SMA 20 on 3 prices: (100 + 110 + 120) / 3 = 110.00
        assertThat(result.getMovingAverage5Day()).isEqualByComparingTo("110.00");
        assertThat(result.getMovingAverage20Day()).isEqualByComparingTo("110.00");
        // Extremes
        assertThat(result.getHighestPrice()).isEqualByComparingTo("125.00");
        assertThat(result.getLowestPrice()).isEqualByComparingTo("95.00");
        // RSI is neutral 50.00 when n <= 14
        assertThat(result.getRsi14()).isEqualByComparingTo("50.00");
        // Score = 0 (SMA5 == SMA20, RSI neutral) -> HOLD
        assertThat(result.getSignal()).isEqualTo("HOLD");
        assertThat(result.getSignalReason()).contains("SMA5 equal to SMA20").contains("Score 0");
    }

    @Test
    @DisplayName("Flat prices over 21 days: 0% change, identical SMAs, zero volatility, and calculated RSI")
    void analyzeStock_flatPrices() {
        when(stockRepository.findBySymbol("TEST")).thenReturn(Optional.of(testStock));

        List<StockPrice> pricesDesc = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 21);
        for (int i = 0; i < 21; i++) {
            pricesDesc.add(createPrice(baseDate.minusDays(i), 100, 102, 98, 100, 5000));
        }
        when(stockPriceRepository.findLatestPricesBySymbol("TEST")).thenReturn(pricesDesc);

        AnalysisDto result = analysisService.analyzeStock("TEST");

        assertThat(result.getLatestClosePrice()).isEqualByComparingTo("100.00");
        assertThat(result.getPercentageChange()).isEqualByComparingTo("0.00");
        assertThat(result.getMovingAverage5Day()).isEqualByComparingTo("100.00");
        assertThat(result.getMovingAverage20Day()).isEqualByComparingTo("100.00");
        assertThat(result.getHighestPrice()).isEqualByComparingTo("102.00");
        assertThat(result.getLowestPrice()).isEqualByComparingTo("98.00");
        assertThat(result.getVolatility()).isEqualByComparingTo("0.00");
        // Since loss is 0 across all days, RSI calculates as 100.00
        assertThat(result.getRsi14()).isEqualByComparingTo("100.00");
        // Score: SMA5 == SMA20 (0) and RSI > 65 (-1) -> Score -1 -> SELL
        assertThat(result.getSignal()).isEqualTo("SELL");
    }

    @Test
    @DisplayName("Uptrend with balanced oscillation: SMA5 > SMA20 and neutral RSI produce a BUY signal")
    void analyzeStock_uptrend_producesBuySignal() {
        when(stockRepository.findBySymbol("TEST")).thenReturn(Optional.of(testStock));

        // 25 prices trending upward from 100 to 112 with small pullbacks
        double[] closes = new double[]{
                100, 98, 101, 99, 102, 100, 103, 101, 104, 102,
                105, 103, 106, 104, 107, 105, 108, 106, 109, 107,
                110, 108, 111, 109, 112
        };

        List<StockPrice> pricesDesc = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 25);
        for (int i = closes.length - 1; i >= 0; i--) {
            double c = closes[i];
            pricesDesc.add(createPrice(baseDate.minusDays(closes.length - 1 - i), c - 1, c + 2, c - 2, c, 10000));
        }
        when(stockPriceRepository.findLatestPricesBySymbol("TEST")).thenReturn(pricesDesc);

        AnalysisDto result = analysisService.analyzeStock("TEST");

        // SMA5 = (110 + 108 + 111 + 109 + 112) / 5 = 110.00
        assertThat(result.getMovingAverage5Day()).isEqualByComparingTo("110.00");
        // SMA20 = 106.00
        assertThat(result.getMovingAverage20Day()).isEqualByComparingTo("106.00");
        // RSI is neutral (60.93)
        assertThat(result.getRsi14()).isEqualByComparingTo("60.93");
        // SMA5 > SMA20 (+1) and neutral RSI (0) -> Score 1 -> BUY
        assertThat(result.getSignal()).isEqualTo("BUY");
        assertThat(result.getSignalReason()).contains("SMA5 above SMA20").contains("Score 1");
    }

    @Test
    @DisplayName("Downtrend with balanced oscillation: SMA5 < SMA20 and neutral RSI produce a SELL signal")
    void analyzeStock_downtrend_producesSellSignal() {
        when(stockRepository.findBySymbol("TEST")).thenReturn(Optional.of(testStock));

        // 25 prices trending downward from 112 to 100
        double[] closes = new double[]{
                112, 114, 111, 113, 110, 112, 109, 111, 108, 110,
                107, 109, 106, 108, 105, 107, 104, 106, 103, 105,
                102, 104, 101, 103, 100
        };

        List<StockPrice> pricesDesc = new ArrayList<>();
        LocalDate baseDate = LocalDate.of(2024, 1, 25);
        for (int i = closes.length - 1; i >= 0; i--) {
            double c = closes[i];
            pricesDesc.add(createPrice(baseDate.minusDays(closes.length - 1 - i), c + 1, c + 2, c - 2, c, 10000));
        }
        when(stockPriceRepository.findLatestPricesBySymbol("TEST")).thenReturn(pricesDesc);

        AnalysisDto result = analysisService.analyzeStock("TEST");

        // SMA5 = (102 + 104 + 101 + 103 + 100) / 5 = 102.00
        assertThat(result.getMovingAverage5Day()).isEqualByComparingTo("102.00");
        // SMA20 = 106.00
        assertThat(result.getMovingAverage20Day()).isEqualByComparingTo("106.00");
        // RSI is neutral (39.07)
        assertThat(result.getRsi14()).isEqualByComparingTo("39.07");
        // SMA5 < SMA20 (-1) and neutral RSI (0) -> Score -1 -> SELL
        assertThat(result.getSignal()).isEqualTo("SELL");
        assertThat(result.getSignalReason()).contains("SMA5 below SMA20").contains("Score -1");
    }

    @Test
    @DisplayName("Unknown stock symbol throws ResourceNotFoundException")
    void analyzeStock_unknownSymbol_throwsException() {
        when(stockRepository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.analyzeStock("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stock not found: UNKNOWN");
    }

    @Test
    @DisplayName("Known stock with no historical prices throws ResourceNotFoundException")
    void analyzeStock_noPriceData_throwsException() {
        when(stockRepository.findBySymbol("TEST")).thenReturn(Optional.of(testStock));
        when(stockPriceRepository.findLatestPricesBySymbol("TEST")).thenReturn(List.of());

        assertThatThrownBy(() -> analysisService.analyzeStock("TEST"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No price data found for: TEST");
    }
}
