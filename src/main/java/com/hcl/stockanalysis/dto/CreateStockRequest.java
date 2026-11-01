package com.hcl.stockanalysis.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateStockRequest {

    @NotBlank(message = "Stock symbol is required")
    @Size(max = 15, message = "Symbol must be up to 15 characters")
    @Pattern(regexp = "^[A-Z0-9.-]{1,15}$", message = "Symbol must contain letters, digits, dot and hyphen only, up to 15 characters, converted to uppercase (no spaces or lowercase letters)")
    private String symbol;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Sector is required")
    @Size(min = 2, max = 50, message = "Sector must be between 2 and 50 characters")
    private String sector;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    public CreateStockRequest() {
    }

    public CreateStockRequest(String symbol, String name, String sector, BigDecimal startingPrice) {
        this.symbol = symbol;
        this.name = name;
        this.sector = sector;
        this.startingPrice = startingPrice;
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
    }

    public String getCompanyName() {
        return name;
    }

    public void setCompanyName(String companyName) {
        if (this.name == null || this.name.isBlank()) {
            this.name = companyName;
        }
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }
}
