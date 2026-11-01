package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.AnalysisDto;
import com.hcl.stockanalysis.dto.PredictionDto;
import com.hcl.stockanalysis.dto.PredictionPointDto;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public AnalysisService(StockRepository stockRepository, StockPriceRepository stockPriceRepository) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    public AnalysisDto analyzeStock(String symbol) {
        var stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));

        List<StockPrice> pricesDesc = stockPriceRepository.findLatestPricesBySymbol(symbol.toUpperCase());
        if (pricesDesc.isEmpty()) {
            throw new ResourceNotFoundException("No price data found for: " + symbol);
        }

        // Sorted ascending by date
        List<StockPrice> sortedAsc = pricesDesc.stream()
                .sorted(Comparator.comparing(StockPrice::getPriceDate))
                .toList();

        int n = sortedAsc.size();
        StockPrice latestPriceObj = sortedAsc.get(n - 1);
        BigDecimal latestClose = latestPriceObj.getClosePrice();
        BigDecimal previousClose = (n > 1) ? sortedAsc.get(n - 2).getClosePrice() : latestClose;

        BigDecimal percentageChange = calculatePercentageChange(previousClose, latestClose);
        BigDecimal ma5 = calculateMovingAverage(sortedAsc, 5);
        BigDecimal ma20 = calculateMovingAverage(sortedAsc, 20);

        BigDecimal highest = sortedAsc.stream()
                .map(StockPrice::getHighPrice)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal lowest = sortedAsc.stream()
                .map(StockPrice::getLowPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        List<String> dates = new ArrayList<>(n);
        List<BigDecimal> closePrices = new ArrayList<>(n);
        List<BigDecimal> sma5Series = new ArrayList<>(n);
        List<BigDecimal> sma20Series = new ArrayList<>(n);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < n; i++) {
            dates.add(sortedAsc.get(i).getPriceDate().format(dtf));
            closePrices.add(sortedAsc.get(i).getClosePrice());

            // SMA 5
            int start5 = Math.max(0, i - 4);
            BigDecimal sum5 = BigDecimal.ZERO;
            int count5 = 0;
            for (int j = start5; j <= i; j++) {
                sum5 = sum5.add(sortedAsc.get(j).getClosePrice());
                count5++;
            }
            sma5Series.add(sum5.divide(BigDecimal.valueOf(count5), 2, RoundingMode.HALF_UP));

            // SMA 20
            int start20 = Math.max(0, i - 19);
            BigDecimal sum20 = BigDecimal.ZERO;
            int count20 = 0;
            for (int j = start20; j <= i; j++) {
                sum20 = sum20.add(sortedAsc.get(j).getClosePrice());
                count20++;
            }
            sma20Series.add(sum20.divide(BigDecimal.valueOf(count20), 2, RoundingMode.HALF_UP));
        }

        BigDecimal rsi14 = calculateRSI(sortedAsc, 14);
        BigDecimal volatility = calculateVolatility(sortedAsc);

        String[] signalAndReason = generateSignal(latestClose, ma5, ma20, rsi14, percentageChange);

        return AnalysisDto.builder()
                .symbol(stock.getSymbol())
                .companyName(stock.getCompanyName())
                .latestClosePrice(latestClose)
                .percentageChange(percentageChange)
                .movingAverage5Day(ma5)
                .movingAverage20Day(ma20)
                .highestPrice(highest)
                .lowestPrice(lowest)
                .rsi14(rsi14)
                .volatility(volatility)
                .dates(dates)
                .closePrices(closePrices)
                .sma5Series(sma5Series)
                .sma20Series(sma20Series)
                .signal(signalAndReason[0])
                .signalReason(signalAndReason[1])
                .dataSource(stock.getDataSource())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }

    public com.hcl.stockanalysis.dto.StockSummaryDto summarizeStock(com.hcl.stockanalysis.entity.Stock stock, List<StockPrice> sortedAsc) {
        if (sortedAsc == null || sortedAsc.isEmpty()) {
            return new com.hcl.stockanalysis.dto.StockSummaryDto(
                    stock.getSymbol(),
                    stock.getCompanyName(),
                    stock.getSector(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "HOLD",
                    "No price history available",
                    stock.getDataSource(),
                    stock.getLastUpdated()
            );
        }

        int n = sortedAsc.size();
        StockPrice latestPriceObj = sortedAsc.get(n - 1);
        BigDecimal latestClose = latestPriceObj.getClosePrice();
        BigDecimal previousClose = (n > 1) ? sortedAsc.get(n - 2).getClosePrice() : latestClose;

        BigDecimal percentageChange = calculatePercentageChange(previousClose, latestClose);
        BigDecimal ma5 = calculateMovingAverage(sortedAsc, 5);
        BigDecimal ma20 = calculateMovingAverage(sortedAsc, 20);
        BigDecimal rsi14 = calculateRSI(sortedAsc, 14);

        String[] signalAndReason = generateSignal(latestClose, ma5, ma20, rsi14, percentageChange);

        return new com.hcl.stockanalysis.dto.StockSummaryDto(
                stock.getSymbol(),
                stock.getCompanyName(),
                stock.getSector(),
                latestClose,
                percentageChange,
                signalAndReason[0],
                signalAndReason[1],
                stock.getDataSource(),
                stock.getLastUpdated()
        );
    }

    public PredictionDto predictStock(String symbol) {
        var stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));

        List<StockPrice> pricesDesc = stockPriceRepository.findLatestPricesBySymbol(symbol.toUpperCase());
        if (pricesDesc.isEmpty()) {
            throw new ResourceNotFoundException("No price data found for: " + symbol);
        }

        List<StockPrice> sortedAsc = pricesDesc.stream()
                .sorted(Comparator.comparing(StockPrice::getPriceDate))
                .toList();

        int n = sortedAsc.size();
        int window = Math.min(20, n);
        List<StockPrice> regCloses = sortedAsc.subList(n - window, n);

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < window; i++) {
            double x = i;
            double y = regCloses.get(i).getClosePrice().doubleValue();
            sumX += x;
            sumY += y;
            sumXY += (x * y);
            sumX2 += (x * x);
        }

        double denominator = (window * sumX2 - sumX * sumX);
        double slope = (denominator != 0) ? (window * sumXY - sumX * sumY) / denominator : 0.0;
        double intercept = (sumY - slope * sumX) / window;

        LocalDate lastDate = sortedAsc.get(n - 1).getPriceDate();
        BigDecimal currentPrice = sortedAsc.get(n - 1).getClosePrice();

        List<PredictionPointDto> predictions = new ArrayList<>(7);
        BigDecimal finalPredicted = currentPrice;
        LocalDate nextDate = lastDate;

        for (int k = 1; k <= 7; k++) {
            nextDate = nextDate.plusDays(1);
            while (nextDate.getDayOfWeek() == DayOfWeek.SATURDAY || nextDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                nextDate = nextDate.plusDays(1);
            }
            double xPred = (window - 1) + k;
            double yPred = slope * xPred + intercept;
            if (yPred < 1.0) yPred = 1.0;
            BigDecimal predPrice = BigDecimal.valueOf(yPred).setScale(2, RoundingMode.HALF_UP);
            predictions.add(new PredictionPointDto(nextDate, predPrice));
            finalPredicted = predPrice;
        }

        String trend = (slope >= 0) ? "UP" : "DOWN";
        BigDecimal changePercent = calculatePercentageChange(currentPrice, finalPredicted);

        return new PredictionDto(
                stock.getSymbol(),
                stock.getCompanyName(),
                trend,
                currentPrice,
                finalPredicted,
                changePercent,
                predictions
        );
    }

    private BigDecimal calculatePercentageChange(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(previous)
                .divide(previous, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMovingAverage(List<StockPrice> sortedAsc, int days) {
        int size = sortedAsc.size();
        if (size < days) days = size;
        List<StockPrice> recent = sortedAsc.subList(size - days, size);
        BigDecimal sum = recent.stream()
                .map(StockPrice::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRSI(List<StockPrice> sortedAsc, int period) {
        int n = sortedAsc.size();
        if (n <= period) {
            return BigDecimal.valueOf(50.0).setScale(2, RoundingMode.HALF_UP);
        }

        double gainSum = 0.0;
        double lossSum = 0.0;

        for (int i = 1; i <= period; i++) {
            double diff = sortedAsc.get(i).getClosePrice().doubleValue() - sortedAsc.get(i - 1).getClosePrice().doubleValue();
            if (diff >= 0) gainSum += diff;
            else lossSum += Math.abs(diff);
        }

        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;

        for (int i = period + 1; i < n; i++) {
            double diff = sortedAsc.get(i).getClosePrice().doubleValue() - sortedAsc.get(i - 1).getClosePrice().doubleValue();
            double gain = (diff >= 0) ? diff : 0.0;
            double loss = (diff < 0) ? Math.abs(diff) : 0.0;

            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        if (avgLoss == 0) {
            return BigDecimal.valueOf(100.0).setScale(2, RoundingMode.HALF_UP);
        }

        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        return BigDecimal.valueOf(rsi).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolatility(List<StockPrice> sortedAsc) {
        int n = sortedAsc.size();
        if (n < 2) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        double[] returns = new double[n - 1];
        double sum = 0.0;

        for (int i = 1; i < n; i++) {
            double prev = sortedAsc.get(i - 1).getClosePrice().doubleValue();
            double curr = sortedAsc.get(i).getClosePrice().doubleValue();
            returns[i - 1] = (prev > 0) ? (curr - prev) / prev : 0.0;
            sum += returns[i - 1];
        }

        double mean = sum / (n - 1);
        double varianceSum = 0.0;
        for (double r : returns) {
            varianceSum += Math.pow(r - mean, 2);
        }

        double stdDev = Math.sqrt(varianceSum / (n - 1));
        // Expressed as percentage volatility
        double volPct = stdDev * 100.0;
        return BigDecimal.valueOf(volPct).setScale(2, RoundingMode.HALF_UP);
    }

    private String[] generateSignal(BigDecimal price, BigDecimal ma5, BigDecimal ma20,
                                     BigDecimal rsi, BigDecimal percentageChange) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (ma5.compareTo(ma20) > 0) {
            score += 1;
            reasons.add("SMA5 above SMA20");
        } else if (ma5.compareTo(ma20) < 0) {
            score -= 1;
            reasons.add("SMA5 below SMA20");
        } else {
            reasons.add("SMA5 equal to SMA20");
        }

        double rsiVal = rsi.doubleValue();
        if (rsiVal < 35.0) {
            score += 1;
            reasons.add("RSI " + rsi + " (oversold)");
        } else if (rsiVal > 65.0) {
            score -= 1;
            reasons.add("RSI " + rsi + " (overbought)");
        } else {
            reasons.add("RSI " + rsi + " (neutral)");
        }

        String signal = (score >= 1) ? "BUY" : (score <= -1 ? "SELL" : "HOLD");
        String reason = String.join(" and ", reasons) + " (Score " + score + ")";

        return new String[]{signal, reason};
    }
}
