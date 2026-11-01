package com.hcl.stockanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {
    private Long id;
    private String symbol;
    private String companyName;
    private String sector;
    private String dataSource = "SAMPLE";
    private java.time.LocalDateTime lastUpdated;

    public StockDto() {
    }

    public StockDto(Long id, String symbol, String companyName, String sector) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.dataSource = "SAMPLE";
    }

    public StockDto(Long id, String symbol, String companyName, String sector, String dataSource, java.time.LocalDateTime lastUpdated) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.dataSource = (dataSource != null) ? dataSource : "SAMPLE";
        this.lastUpdated = lastUpdated;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getDataSource() {
        return dataSource != null ? dataSource : "SAMPLE";
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public java.time.LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(java.time.LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
