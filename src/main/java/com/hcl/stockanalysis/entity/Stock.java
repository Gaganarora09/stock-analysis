package com.hcl.stockanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String companyName;

    private String sector;

    @Column
    private String dataSource = "SAMPLE";

    private java.time.LocalDateTime lastUpdated;

    private String marketSymbol;

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<StockPrice> prices = new ArrayList<>();

    public Stock() {
    }

    public Stock(Long id, String symbol, String companyName, String sector, List<StockPrice> prices) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.sector = sector;
        this.prices = (prices != null) ? prices : new ArrayList<>();
    }

    public static StockBuilder builder() {
        return new StockBuilder();
    }

    public static class StockBuilder {
        private Long id;
        private String symbol;
        private String companyName;
        private String sector;
        private String dataSource = "SAMPLE";
        private java.time.LocalDateTime lastUpdated;
        private String marketSymbol;
        private List<StockPrice> prices = new ArrayList<>();

        public StockBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public StockBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public StockBuilder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public StockBuilder sector(String sector) {
            this.sector = sector;
            return this;
        }

        public StockBuilder dataSource(String dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public StockBuilder lastUpdated(java.time.LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public StockBuilder marketSymbol(String marketSymbol) {
            this.marketSymbol = marketSymbol;
            return this;
        }

        public StockBuilder prices(List<StockPrice> prices) {
            this.prices = prices;
            return this;
        }

        public Stock build() {
            Stock s = new Stock(id, symbol, companyName, sector, prices);
            if (this.dataSource != null) s.setDataSource(this.dataSource);
            s.setLastUpdated(this.lastUpdated);
            s.setMarketSymbol(this.marketSymbol);
            return s;
        }
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

    public List<StockPrice> getPrices() {
        return prices;
    }

    public void setPrices(List<StockPrice> prices) {
        this.prices = prices;
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

    public String getMarketSymbol() {
        return marketSymbol;
    }

    public void setMarketSymbol(String marketSymbol) {
        this.marketSymbol = marketSymbol;
    }
}
