package com.hcl.stockanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcl.stockanalysis.dto.DailyCloseDto;
import com.hcl.stockanalysis.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class AlphaVantageClient implements MarketDataClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);
    private static final String BASE_URL = "https://www.alphavantage.co";

    private final String apiKey;
    private final String defaultSuffix;
    private final boolean enabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AlphaVantageClient(
            @Value("${app.marketdata.api-key:}") String apiKey,
            @Value("${app.marketdata.symbol-suffix:.BSE}") String defaultSuffix,
            @Value("${app.marketdata.enabled:false}") boolean enabled,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.defaultSuffix = defaultSuffix != null ? defaultSuffix.trim() : ".BSE";
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Override
    public List<DailyCloseDto> fetchDailyCloses(String symbol) {
        if (!enabled || apiKey.isEmpty()) {
            return List.of();
        }

        String apiSymbol = symbol.contains(".") ? symbol : symbol + defaultSuffix;

        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", apiSymbol)
                            .queryParam("outputsize", "compact")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Empty response from Alpha Vantage for symbol: {}", symbol);
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);

            // Check for rate limit note or information
            if (root.has("Note")) {
                String note = root.get("Note").asText();
                log.warn("Alpha Vantage rate limit note received: {}", note);
                throw new RateLimitExceededException("Alpha Vantage call frequency limit reached");
            }
            if (root.has("Information")) {
                String info = root.get("Information").asText();
                log.warn("Alpha Vantage information note received: {}", info);
                throw new RateLimitExceededException("Alpha Vantage daily limit reached");
            }
            if (root.has("Error Message")) {
                log.warn("Alpha Vantage error for symbol {}: {}", symbol, root.get("Error Message").asText());
                return List.of();
            }

            JsonNode timeSeries = root.get("Time Series (Daily)");
            if (timeSeries == null || !timeSeries.isObject()) {
                log.warn("No 'Time Series (Daily)' found in Alpha Vantage response for symbol: {}", symbol);
                return List.of();
            }

            List<DailyCloseDto> result = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                try {
                    LocalDate date = LocalDate.parse(entry.getKey(), formatter);
                    JsonNode dayData = entry.getValue();

                    BigDecimal open = new BigDecimal(dayData.get("1. open").asText());
                    BigDecimal high = new BigDecimal(dayData.get("2. high").asText());
                    BigDecimal low = new BigDecimal(dayData.get("3. low").asText());
                    BigDecimal close = new BigDecimal(dayData.get("4. close").asText());
                    Long volume = dayData.has("5. volume") ? dayData.get("5. volume").asLong() : 1000000L;

                    result.add(new DailyCloseDto(date, close, open, high, low, volume));
                } catch (Exception ex) {
                    // skip malformed single day
                }
            }

            return result;
        } catch (RateLimitExceededException rle) {
            throw rle;
        } catch (Exception e) {
            log.warn("Failed to fetch market data from Alpha Vantage for symbol {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
