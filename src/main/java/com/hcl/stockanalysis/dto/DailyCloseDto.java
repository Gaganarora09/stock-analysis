package com.hcl.stockanalysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCloseDto(
        LocalDate date,
        BigDecimal close,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        Long volume
) {
    public DailyCloseDto(LocalDate date, BigDecimal close) {
        this(date, close, close, close, close, 1_000_000L);
    }
}
