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
public class PredictionDto {
    private String symbol;
    private String companyName;
    private String trend; // "UP" or "DOWN"
    private BigDecimal currentPrice;
    private BigDecimal predictedFinalPrice;
    private BigDecimal predictedChangePercent;
    private List<PredictionPointDto> predictions;

    public PredictionDto() {
    }

    public PredictionDto(String symbol, String companyName, String trend,
                         BigDecimal currentPrice, BigDecimal predictedFinalPrice,
                         BigDecimal predictedChangePercent, List<PredictionPointDto> predictions) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.trend = trend;
        this.currentPrice = currentPrice;
        this.predictedFinalPrice = predictedFinalPrice;
        this.predictedChangePercent = predictedChangePercent;
        this.predictions = predictions;
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

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getPredictedFinalPrice() {
        return predictedFinalPrice;
    }

    public void setPredictedFinalPrice(BigDecimal predictedFinalPrice) {
        this.predictedFinalPrice = predictedFinalPrice;
    }

    public BigDecimal getPredictedChangePercent() {
        return predictedChangePercent;
    }

    public void setPredictedChangePercent(BigDecimal predictedChangePercent) {
        this.predictedChangePercent = predictedChangePercent;
    }

    public List<PredictionPointDto> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<PredictionPointDto> predictions) {
        this.predictions = predictions;
    }
}
