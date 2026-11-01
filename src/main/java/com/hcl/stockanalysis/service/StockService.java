package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.StockDto;
import com.hcl.stockanalysis.dto.StockPriceDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.StockPrice;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.repository.StockPriceRepository;
import com.hcl.stockanalysis.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

    public StockService(StockRepository stockRepository, StockPriceRepository stockPriceRepository) {
        this.stockRepository = stockRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    public List<StockDto> getAllStocks() {
        return stockRepository.findAll().stream()
                .map(this::toStockDto)
                .collect(Collectors.toList());
    }

    public StockDto getStockBySymbol(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));
        return toStockDto(stock);
    }

    public List<StockPriceDto> getPricesBySymbol(String symbol) {
        if (!stockRepository.existsBySymbol(symbol.toUpperCase())) {
            throw new ResourceNotFoundException("Stock not found: " + symbol);
        }
        return stockPriceRepository.findByStockSymbolOrderByPriceDateDesc(symbol.toUpperCase())
                .stream()
                .map(this::toStockPriceDto)
                .collect(Collectors.toList());
    }

    private StockDto toStockDto(Stock stock) {
        return new StockDto(stock.getId(), stock.getSymbol(), stock.getCompanyName(), stock.getSector(), stock.getDataSource(), stock.getLastUpdated());
    }

    private StockPriceDto toStockPriceDto(StockPrice sp) {
        return new StockPriceDto(
                sp.getId(),
                sp.getStock().getSymbol(),
                sp.getPriceDate(),
                sp.getOpenPrice(),
                sp.getClosePrice(),
                sp.getHighPrice(),
                sp.getLowPrice(),
                sp.getVolume()
        );
    }
}
