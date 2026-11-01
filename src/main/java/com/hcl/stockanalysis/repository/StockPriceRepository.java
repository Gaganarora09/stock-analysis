package com.hcl.stockanalysis.repository;

import com.hcl.stockanalysis.entity.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    List<StockPrice> findByStockSymbolOrderByPriceDateDesc(String symbol);

    Optional<StockPrice> findByStockAndPriceDate(com.hcl.stockanalysis.entity.Stock stock, LocalDate priceDate);

    void deleteByStock(com.hcl.stockanalysis.entity.Stock stock);

    @Query("SELECT sp FROM StockPrice sp WHERE sp.stock.symbol = :symbol ORDER BY sp.priceDate DESC")
    List<StockPrice> findLatestPricesBySymbol(@Param("symbol") String symbol);

    @Query("SELECT sp FROM StockPrice sp WHERE sp.stock.symbol = :symbol AND sp.priceDate BETWEEN :startDate AND :endDate ORDER BY sp.priceDate ASC")
    List<StockPrice> findBySymbolAndDateRange(
            @Param("symbol") String symbol,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT sp FROM StockPrice sp JOIN FETCH sp.stock ORDER BY sp.priceDate ASC")
    List<StockPrice> findAllPricesWithStockAsc();
}
