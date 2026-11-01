package com.hcl.stockanalysis.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockSummaryDto {

    private String symbol;
    private String name;
    private String companyName;
    private String sector;
    private BigDecimal latestPrice;
    private BigDecimal latestClosePrice;
    private BigDecimal percentChange;
    private BigDecimal percentageChange;
    private String signal;
    private String signalReason;
    private String dataSource;
    private LocalDateTime lastUpdated;

    public StockSummaryDto() {
    }

    public StockSummaryDto(String symbol, String companyName, String sector,
                           BigDecimal latestPrice, BigDecimal percentageChange,
                           String signal, String signalReason,
                           String dataSource, LocalDateTime lastUpdated) {
        this.symbol = symbol;
        this.name = companyName;
        this.companyName = companyName;
        this.sector = sector;
        this.latestPrice = latestPrice;
        this.latestClosePrice = latestPrice;
        this.percentChange = percentageChange;
        this.percentageChange = percentageChange;
        this.signal = signal;
        this.signalReason = signalReason;
        this.dataSource = dataSource != null ? dataSource : "SAMPLE";
        this.lastUpdated = lastUpdated;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.companyName = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        this.name = companyName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
        this.latestClosePrice = latestPrice;
    }

    public BigDecimal getLatestClosePrice() {
        return latestClosePrice;
    }

    public void setLatestClosePrice(BigDecimal latestClosePrice) {
        this.latestClosePrice = latestClosePrice;
        this.latestPrice = latestClosePrice;
    }

    public BigDecimal getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(BigDecimal percentChange) {
        this.percentChange = percentChange;
        this.percentageChange = percentChange;
    }

    public BigDecimal getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(BigDecimal percentageChange) {
        this.percentageChange = percentageChange;
        this.percentChange = percentageChange;
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

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
