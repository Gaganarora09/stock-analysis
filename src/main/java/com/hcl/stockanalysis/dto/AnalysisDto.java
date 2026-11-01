package com.hcl.stockanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisDto {
    private String symbol;
    private String companyName;
    private BigDecimal latestClosePrice;
    private BigDecimal percentageChange;
    private BigDecimal movingAverage5Day;
    private BigDecimal movingAverage20Day;
    private BigDecimal highestPrice;
    private BigDecimal lowestPrice;
    private BigDecimal rsi14;
    private BigDecimal volatility;
    private List<String> dates;
    private List<BigDecimal> closePrices;
    private List<BigDecimal> sma5Series;
    private List<BigDecimal> sma20Series;
    private String signal; // "BUY", "SELL", or "HOLD"
    private String signalReason;

    public AnalysisDto() {
    }

    public AnalysisDto(String symbol, String companyName, BigDecimal latestClosePrice,
                       BigDecimal percentageChange, BigDecimal movingAverage5Day,
                       BigDecimal movingAverage20Day, BigDecimal highestPrice,
                       BigDecimal lowestPrice, BigDecimal rsi14, BigDecimal volatility,
                       List<String> dates, List<BigDecimal> closePrices,
                       List<BigDecimal> sma5Series, List<BigDecimal> sma20Series,
                       String signal, String signalReason) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.latestClosePrice = latestClosePrice;
        this.percentageChange = percentageChange;
        this.movingAverage5Day = movingAverage5Day;
        this.movingAverage20Day = movingAverage20Day;
        this.highestPrice = highestPrice;
        this.lowestPrice = lowestPrice;
        this.rsi14 = rsi14;
        this.volatility = volatility;
        this.dates = dates;
        this.closePrices = closePrices;
        this.sma5Series = sma5Series;
        this.sma20Series = sma20Series;
        this.signal = signal;
        this.signalReason = signalReason;
    }

    public static AnalysisDtoBuilder builder() {
        return new AnalysisDtoBuilder();
    }

    public static class AnalysisDtoBuilder {
        private String symbol;
        private String companyName;
        private BigDecimal latestClosePrice;
        private BigDecimal percentageChange;
        private BigDecimal movingAverage5Day;
        private BigDecimal movingAverage20Day;
        private BigDecimal highestPrice;
        private BigDecimal lowestPrice;
        private BigDecimal rsi14;
        private BigDecimal volatility;
        private List<String> dates;
        private List<BigDecimal> closePrices;
        private List<BigDecimal> sma5Series;
        private List<BigDecimal> sma20Series;
        private String signal;
        private String signalReason;

        public AnalysisDtoBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public AnalysisDtoBuilder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public AnalysisDtoBuilder latestClosePrice(BigDecimal latestClosePrice) {
            this.latestClosePrice = latestClosePrice;
            return this;
        }

        public AnalysisDtoBuilder percentageChange(BigDecimal percentageChange) {
            this.percentageChange = percentageChange;
            return this;
        }

        public AnalysisDtoBuilder movingAverage5Day(BigDecimal movingAverage5Day) {
            this.movingAverage5Day = movingAverage5Day;
            return this;
        }

        public AnalysisDtoBuilder movingAverage20Day(BigDecimal movingAverage20Day) {
            this.movingAverage20Day = movingAverage20Day;
            return this;
        }

        public AnalysisDtoBuilder highestPrice(BigDecimal highestPrice) {
            this.highestPrice = highestPrice;
            return this;
        }

        public AnalysisDtoBuilder lowestPrice(BigDecimal lowestPrice) {
            this.lowestPrice = lowestPrice;
            return this;
        }

        public AnalysisDtoBuilder rsi14(BigDecimal rsi14) {
            this.rsi14 = rsi14;
            return this;
        }

        public AnalysisDtoBuilder volatility(BigDecimal volatility) {
            this.volatility = volatility;
            return this;
        }

        public AnalysisDtoBuilder dates(List<String> dates) {
            this.dates = dates;
            return this;
        }

        public AnalysisDtoBuilder closePrices(List<BigDecimal> closePrices) {
            this.closePrices = closePrices;
            return this;
        }

        public AnalysisDtoBuilder sma5Series(List<BigDecimal> sma5Series) {
            this.sma5Series = sma5Series;
            return this;
        }

        public AnalysisDtoBuilder sma20Series(List<BigDecimal> sma20Series) {
            this.sma20Series = sma20Series;
            return this;
        }

        public AnalysisDtoBuilder signal(String signal) {
            this.signal = signal;
            return this;
        }

        public AnalysisDtoBuilder signalReason(String signalReason) {
            this.signalReason = signalReason;
            return this;
        }

        public AnalysisDto build() {
            return new AnalysisDto(symbol, companyName, latestClosePrice, percentageChange,
                    movingAverage5Day, movingAverage20Day, highestPrice, lowestPrice,
                    rsi14, volatility, dates, closePrices, sma5Series, sma20Series,
                    signal, signalReason);
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getLatestClosePrice() {
        return latestClosePrice;
    }

    public void setLatestClosePrice(BigDecimal latestClosePrice) {
        this.latestClosePrice = latestClosePrice;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(BigDecimal percentageChange) {
        this.percentageChange = percentageChange;
    }

    public BigDecimal getMovingAverage5Day() {
        return movingAverage5Day;
    }

    public void setMovingAverage5Day(BigDecimal movingAverage5Day) {
        this.movingAverage5Day = movingAverage5Day;
    }

    public BigDecimal getMovingAverage20Day() {
        return movingAverage20Day;
    }

    public void setMovingAverage20Day(BigDecimal movingAverage20Day) {
        this.movingAverage20Day = movingAverage20Day;
    }

    public BigDecimal getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(BigDecimal highestPrice) {
        this.highestPrice = highestPrice;
    }

    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public BigDecimal getRsi14() {
        return rsi14;
    }

    public void setRsi14(BigDecimal rsi14) {
        this.rsi14 = rsi14;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public void setVolatility(BigDecimal volatility) {
        this.volatility = volatility;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }

    public List<BigDecimal> getClosePrices() {
        return closePrices;
    }

    public void setClosePrices(List<BigDecimal> closePrices) {
        this.closePrices = closePrices;
    }

    public List<BigDecimal> getSma5Series() {
        return sma5Series;
    }

    public void setSma5Series(List<BigDecimal> sma5Series) {
        this.sma5Series = sma5Series;
    }

    public List<BigDecimal> getSma20Series() {
        return sma20Series;
    }

    public void setSma20Series(List<BigDecimal> sma20Series) {
        this.sma20Series = sma20Series;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public String getSignalReason() {
        return signalReason;
    }

    public void setSignalReason(String signalReason) {
        this.signalReason = signalReason;
    }
}
