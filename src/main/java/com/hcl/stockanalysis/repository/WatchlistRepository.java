package com.hcl.stockanalysis.repository;

import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUser(User user);
    Optional<Watchlist> findByUserAndStock(User user, Stock stock);
    boolean existsByUserAndStock(User user, Stock stock);
    void deleteByUserAndStock(User user, Stock stock);
}
