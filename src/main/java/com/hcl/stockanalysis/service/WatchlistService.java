package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.WatchlistItemDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.entity.Watchlist;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.repository.StockRepository;
import com.hcl.stockanalysis.repository.UserRepository;
import com.hcl.stockanalysis.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            UserRepository userRepository,
                            StockRepository stockRepository) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
    }

    public List<WatchlistItemDto> getWatchlist(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return watchlistRepository.findByUser(user).stream()
                .map(w -> new WatchlistItemDto(
                        w.getStock().getSymbol(),
                        w.getStock().getCompanyName(),
                        w.getStock().getSector()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWatchlist(String username, String symbol) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));
        if (!watchlistRepository.existsByUserAndStock(user, stock)) {
            Watchlist entry = new Watchlist(user, stock, LocalDateTime.now());
            watchlistRepository.save(entry);
        }
    }

    @Transactional
    public void removeFromWatchlist(String username, String symbol) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));
        watchlistRepository.deleteByUserAndStock(user, stock);
    }
}
