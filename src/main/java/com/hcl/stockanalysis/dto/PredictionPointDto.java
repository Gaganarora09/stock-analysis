package com.hcl.stockanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionPointDto {
    private LocalDate date;
    private BigDecimal predictedPrice;

    public PredictionPointDto() {
    }

    public PredictionPointDto(LocalDate date, BigDecimal predictedPrice) {
        this.date = date;
        this.predictedPrice = predictedPrice;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(BigDecimal predictedPrice) {
        this.predictedPrice = predictedPrice;
    }
}
