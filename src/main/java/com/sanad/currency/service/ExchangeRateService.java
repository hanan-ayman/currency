package com.sanad.currency.service;

import com.sanad.currency.dto.ExchangeRateResponse;
import com.sanad.currency.dto.OpenExchangeRatesResponse;
import com.sanad.currency.exception.ResourceNotFoundException;
import com.sanad.currency.model.Currency;
import com.sanad.currency.model.ExchangeRate;
import com.sanad.currency.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final WebClient webClient;
    private final CurrencyService currencyService;
    private final ExchangeRateRepository exchangeRateRepository;

    @Value("${openexchangerates.api-key}")
    private String apiKey;
    private final Map<String, Map<String, BigDecimal>> exchangeRateCache = new ConcurrentHashMap<>();

    @Scheduled(fixedRateString = "${app.scheduling.fixed-rate}")
    @Transactional
    public void fetchAndStoreExchangeRates() {
        log.info("Starting scheduled exchange rate update");

        List<Currency> currencies = currencyService.getAllCurrencies().stream()
                .map(response -> currencyService.getCurrencyByCode(response.code()))
                .toList();

        if (currencies.isEmpty()) {
            log.info("No currencies found to update exchange rates");
            return;
        }

        for (Currency baseCurrency : currencies) {
            try {
                fetchAndStoreRatesForCurrency(baseCurrency);
            } catch (Exception e) {
                log.error("Error updating rates for currency {}: {}", baseCurrency.getCode(), e.getMessage());
            }
        }

        log.info("Completed scheduled exchange rate update");
    }

    private void fetchAndStoreRatesForCurrency(Currency baseCurrency) {
        String baseCode = baseCurrency.getCode();

        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest.json")
                        .queryParam("base", baseCode)
                        .queryParam("app_id", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(OpenExchangeRatesResponse.class)
                .doOnSuccess(response -> {
                    log.info("Fetched exchange rates for base currency: {}", baseCode);
                    updateExchangeRates(baseCurrency, response);
                })
                .doOnError(error ->
                        log.error("Error fetching exchange rates for base currency: {}", baseCode, error)
                )
                .subscribe();
    }

    private void updateExchangeRates(Currency baseCurrency, OpenExchangeRatesResponse response) {
        String baseCode = baseCurrency.getCode();
        Map<String, BigDecimal> rates = response.rates();

        // Create a new map to store only the currencies that exist in the database
        Map<String, BigDecimal> validRates = new ConcurrentHashMap<>();

        List<ExchangeRate> exchangeRates = rates.entrySet().stream()
                .map(entry -> {
                    String targetCode = entry.getKey();
                    try {
                        // Skip if the target currency is the same as base or doesn't exist in our database
                        if (baseCurrency.getCode().equals(targetCode)) {
                            return null;
                        }
                        
                        Currency targetCurrency = currencyService.getCurrencyByCode(targetCode);
                        
                        // Check if a rate already exists for this currency pair and timestamp
                        Instant now = Instant.now();
                        Optional<ExchangeRate> existingRate = exchangeRateRepository.findLatestRate(
                                baseCurrency.getCode(), targetCode);
                                
                        if (existingRate.isPresent() && 
                            existingRate.get().getRate().compareTo(entry.getValue()) == 0) {
                            // Rate already exists with the same value, skip saving
                            validRates.put(targetCode, entry.getValue());
                            return null;
                        }
                        
                        ExchangeRate rate = new ExchangeRate();
                        rate.setBaseCurrency(baseCurrency);
                        rate.setTargetCurrency(targetCurrency);
                        rate.setRate(entry.getValue());
                        rate.setTimestamp(now);
                        
                        validRates.put(targetCode, entry.getValue());
                        
                        return rate;
                    } catch (Exception e) {
                        log.warn("Skipping rate for {} to {}: {}", 
                                baseCode, targetCode, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (!validRates.isEmpty()) {
            exchangeRateCache.put(baseCode, validRates);
            if (!exchangeRates.isEmpty()) {
                exchangeRateRepository.saveAll(exchangeRates);
                log.info("Saved {} exchange rates to database for {}", exchangeRates.size(), baseCode);
            }
        } else {
            log.warn("No valid exchange rates found for {}", baseCode);
        }
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getExchangeRate(String baseCurrencyCode, String targetCurrencyCode) {
        // Check if both currencies exist
        Currency baseCurrency = currencyService.getCurrencyByCode(baseCurrencyCode);
        Currency targetCurrency = currencyService.getCurrencyByCode(targetCurrencyCode);

        // 1. Try to get from in-memory map first
        Map<String, BigDecimal> baseRates = exchangeRateCache.get(baseCurrency.getCode());
        if (baseRates != null) {
            BigDecimal rate = baseRates.get(targetCurrency.getCode());
            if (rate != null) {
                return new ExchangeRateResponse(
                        baseCurrency.getCode(),
                        targetCurrency.getCode(),
                        rate,
                        Instant.now()
                );
            }
        }

        // 2. If not in memory, fetch from API
        try {
            log.info("Exchange rate not found in cache. Fetching from API...");
            fetchAndStoreRatesForCurrency(baseCurrency);
            Thread.sleep(1000);

            // Check cache again after API fetch
            baseRates = exchangeRateCache.get(baseCurrency.getCode());
            if (baseRates != null) {
                BigDecimal rate = baseRates.get(targetCurrency.getCode());
                if (rate != null) {
                    return new ExchangeRateResponse(
                            baseCurrency.getCode(),
                            targetCurrency.getCode(),
                            rate,
                            Instant.now()
                    );
                }
            }

            throw new ResourceNotFoundException(
                    String.format("No exchange rate found from %s to %s after API fetch",
                            baseCurrency.getCode(),
                            targetCurrency.getCode())
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResourceNotFoundException("Request was interrupted while fetching rates");
        } catch (Exception e) {
            log.error("Error fetching exchange rate from API", e);
            throw new ResourceNotFoundException(
                    String.format("Failed to fetch exchange rate: %s", e.getMessage())
            );
        }
    }
}
