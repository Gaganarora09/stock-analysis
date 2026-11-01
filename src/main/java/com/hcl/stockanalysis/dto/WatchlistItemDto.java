package com.hcl.stockanalysis.dto;

public class WatchlistItemDto {

    private String symbol;
    private String companyName;
    private String sector;

    public WatchlistItemDto() {
    }

    public WatchlistItemDto(String symbol, String companyName, String sector) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
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
}
