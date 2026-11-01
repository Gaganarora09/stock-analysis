package com.hcl.stockanalysis.dto;

public class MarketRefreshResultDto {
    private int updated;
    private int skipped;
    private int failed;
    private String message;

    public MarketRefreshResultDto() {
    }

    public MarketRefreshResultDto(int updated, int skipped, int failed, String message) {
        this.updated = updated;
        this.skipped = skipped;
        this.failed = failed;
        this.message = message;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
