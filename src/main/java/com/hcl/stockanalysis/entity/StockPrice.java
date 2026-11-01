package com.hcl.stockanalysis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_price", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_price_date", columnNames = {"stock_id", "price_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Stock stock;

    @Column(nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal openPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal highPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lowPrice;

    private Long volume;

    public StockPrice() {
    }

    public StockPrice(Long id, Stock stock, LocalDate priceDate, BigDecimal openPrice,
                      BigDecimal closePrice, BigDecimal highPrice, BigDecimal lowPrice, Long volume) {
        this.id = id;
        this.stock = stock;
        this.priceDate = priceDate;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
    }

    public static StockPriceBuilder builder() {
        return new StockPriceBuilder();
    }

    public static class StockPriceBuilder {
        private Long id;
        private Stock stock;
        private LocalDate priceDate;
        private BigDecimal openPrice;
        private BigDecimal closePrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private Long volume;

        public StockPriceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public StockPriceBuilder stock(Stock stock) {
            this.stock = stock;
            return this;
        }

        public StockPriceBuilder priceDate(LocalDate priceDate) {
            this.priceDate = priceDate;
            return this;
        }

        public StockPriceBuilder openPrice(BigDecimal openPrice) {
            this.openPrice = openPrice;
            return this;
        }

        public StockPriceBuilder closePrice(BigDecimal closePrice) {
            this.closePrice = closePrice;
            return this;
        }

        public StockPriceBuilder highPrice(BigDecimal highPrice) {
            this.highPrice = highPrice;
            return this;
        }

        public StockPriceBuilder lowPrice(BigDecimal lowPrice) {
            this.lowPrice = lowPrice;
            return this;
        }

        public StockPriceBuilder volume(Long volume) {
            this.volume = volume;
            return this;
        }

        public StockPrice build() {
            return new StockPrice(id, stock, priceDate, openPrice, closePrice, highPrice, lowPrice, volume);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }
}
