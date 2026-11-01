package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.WatchlistItemDto;
import com.hcl.stockanalysis.entity.Stock;
import com.hcl.stockanalysis.entity.User;
import com.hcl.stockanalysis.entity.Watchlist;
import com.hcl.stockanalysis.exception.ResourceNotFoundException;
import com.hcl.stockanalysis.repository.StockRepository;
import com.hcl.stockanalysis.repository.UserRepository;
import com.hcl.stockanalysis.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private WatchlistService watchlistService;

    private User alice;
    private User bob;
    private Stock hclStock;
    private Stock tcsStock;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).username("alice").password("hash1").role("ROLE_USER").build();
        bob = User.builder().id(2L).username("bob").password("hash2").role("ROLE_USER").build();

        hclStock = Stock.builder().id(10L).symbol("HCLTECH").companyName("HCL Technologies Ltd").sector("IT").build();
        tcsStock = Stock.builder().id(20L).symbol("TCS").companyName("Tata Consultancy Services Ltd").sector("IT").build();
    }

    @Test
    @DisplayName("Add stock to watchlist saves new Watchlist entity when not already present")
    void addToWatchlist_newStock_savesEntry() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stockRepository.findBySymbol("HCLTECH")).thenReturn(Optional.of(hclStock));
        when(watchlistRepository.existsByUserAndStock(alice, hclStock)).thenReturn(false);

        watchlistService.addToWatchlist("alice", "HCLTECH");

        ArgumentCaptor<Watchlist> captor = ArgumentCaptor.forClass(Watchlist.class);
        verify(watchlistRepository, times(1)).save(captor.capture());

        Watchlist saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(alice);
        assertThat(saved.getStock()).isEqualTo(hclStock);
        assertThat(saved.getAddedAt()).isNotNull();
    }

    @Test
    @DisplayName("Adding same stock twice is idempotent and does not save duplicate entry")
    void addToWatchlist_addTwice_idempotent() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stockRepository.findBySymbol("HCLTECH")).thenReturn(Optional.of(hclStock));
        when(watchlistRepository.existsByUserAndStock(alice, hclStock)).thenReturn(true);

        watchlistService.addToWatchlist("alice", "HCLTECH");

        verify(watchlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding unknown stock symbol throws ResourceNotFoundException")
    void addToWatchlist_unknownSymbol_throwsResourceNotFoundException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stockRepository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.addToWatchlist("alice", "UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stock not found: UNKNOWN");

        verify(watchlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remove stock from watchlist delegates to deleteByUserAndStock")
    void removeFromWatchlist_success_deletesEntry() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stockRepository.findBySymbol("HCLTECH")).thenReturn(Optional.of(hclStock));

        watchlistService.removeFromWatchlist("alice", "HCLTECH");

        verify(watchlistRepository, times(1)).deleteByUserAndStock(alice, hclStock);
    }

    @Test
    @DisplayName("Removing unknown stock symbol throws ResourceNotFoundException")
    void removeFromWatchlist_unknownSymbol_throwsResourceNotFoundException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(stockRepository.findBySymbol("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.removeFromWatchlist("alice", "INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stock not found: INVALID");

        verify(watchlistRepository, never()).deleteByUserAndStock(any(), any());
    }

    @Test
    @DisplayName("Each user sees only their own watchlist entries")
    void getWatchlist_userIsolation() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        Watchlist aliceEntry = new Watchlist(alice, hclStock, LocalDateTime.now());
        Watchlist bobEntry = new Watchlist(bob, tcsStock, LocalDateTime.now());

        when(watchlistRepository.findByUser(alice)).thenReturn(List.of(aliceEntry));
        when(watchlistRepository.findByUser(bob)).thenReturn(List.of(bobEntry));

        List<WatchlistItemDto> aliceList = watchlistService.getWatchlist("alice");
        List<WatchlistItemDto> bobList = watchlistService.getWatchlist("bob");

        // Alice sees only HCLTECH
        assertThat(aliceList).hasSize(1);
        assertThat(aliceList.get(0).getSymbol()).isEqualTo("HCLTECH");

        // Bob sees only TCS
        assertThat(bobList).hasSize(1);
        assertThat(bobList.get(0).getSymbol()).isEqualTo("TCS");
    }
}
