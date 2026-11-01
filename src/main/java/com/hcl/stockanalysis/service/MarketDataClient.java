package com.hcl.stockanalysis.service;

import com.hcl.stockanalysis.dto.DailyCloseDto;
import java.util.List;

public interface MarketDataClient {
    List<DailyCloseDto> fetchDailyCloses(String symbol);
}
