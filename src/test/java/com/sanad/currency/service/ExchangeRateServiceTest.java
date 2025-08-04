package com.sanad.currency.service;

import com.sanad.currency.dto.CurrencyResponse;
import com.sanad.currency.dto.ExchangeRateResponse;
import com.sanad.currency.dto.OpenExchangeRatesResponse;
import com.sanad.currency.exception.ResourceNotFoundException;
import com.sanad.currency.mapper.ExchangeRateMapper;
import com.sanad.currency.model.Currency;
import com.sanad.currency.model.ExchangeRate;
import com.sanad.currency.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateMapper exchangeRateMapper;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Currency usdCurrency;
    private Currency eurCurrency;
    private ExchangeRate exchangeRate;

    @BeforeEach
    void setUp() {
        // Initialize test data
        usdCurrency = new Currency();
        usdCurrency.setId(1L);
        usdCurrency.setCode("USD");
        usdCurrency.setName("US Dollar");

        eurCurrency = new Currency();
        eurCurrency.setId(2L);
        eurCurrency.setCode("EUR");
        eurCurrency.setName("Euro");

        exchangeRate = new ExchangeRate();
        exchangeRate.setId(1L);
        exchangeRate.setBaseCurrency(usdCurrency);
        exchangeRate.setTargetCurrency(eurCurrency);
        exchangeRate.setRate(new BigDecimal("0.85"));
        exchangeRate.setTimestamp(Instant.now());

        // Initialize in-memory cache
        Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
        cache.put("USD", Map.of("EUR", new BigDecimal("0.85")));
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateCache", cache);
        
        // Set test API key
        ReflectionTestUtils.setField(exchangeRateService, "apiKey", "test-api-key");
    }

    @Test
    void getExchangeRate_WhenInCache_ShouldReturnCachedRate() {
        // Arrange - Set up the cache with the required currency objects
        Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();
        Map<String, BigDecimal> usdRates = new ConcurrentHashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        cache.put("USD", usdRates);
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateCache", cache);
        
        // Mock the required currency service calls
        when(currencyService.getCurrencyByCode("USD")).thenReturn(usdCurrency);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eurCurrency);
        
        // Act
        ExchangeRateResponse result = exchangeRateService.getExchangeRate("USD", "EUR");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("0.85"), result.rate());
        
        // Verify no repository or web client calls were made
        verifyNoInteractions(exchangeRateRepository, webClient);
    }

    @Test
    void getExchangeRate_WhenNotInCacheButInDatabase_ShouldReturnFromDatabase() {
        // Arrange
        when(currencyService.getCurrencyByCode("USD")).thenReturn(usdCurrency);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eurCurrency);

        // Mock WebClient to return the rate
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OpenExchangeRatesResponse.class))
                .thenReturn(Mono.just(new OpenExchangeRatesResponse("USD", 
                        Map.of("EUR", new BigDecimal("0.85")), 
                        Instant.now().getEpochSecond())));

        // Mock repository to return the saved exchange rate
        when(exchangeRateRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ExchangeRate> savedRates = invocation.getArgument(0);
            savedRates.forEach(rate -> rate.setId(1L));
            return savedRates;
        });

        // Clear cache for this test
        Map<String, Map<String, BigDecimal>> emptyCache = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateCache", emptyCache);

        // Act
        ExchangeRateResponse result = exchangeRateService.getExchangeRate("USD", "EUR");

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("0.85"), result.rate());
        
        // Verify the exact number of interactions
        verify(currencyService, times(1)).getCurrencyByCode("USD");
        // The second call to getCurrencyByCode("EUR") happens in updateExchangeRates
        verify(currencyService, times(2)).getCurrencyByCode("EUR");
        
        // Verify WebClient was called to fetch the rate
        verify(webClient, times(1)).get();
        
        // Verify the rate was saved to the repository
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
    }

    @Test
    void getExchangeRate_WhenRateNotFound_ShouldThrowException() {
        // Arrange
        when(currencyService.getCurrencyByCode("USD")).thenReturn(usdCurrency);
        when(currencyService.getCurrencyByCode("EUR")).thenReturn(eurCurrency);

        // Mock WebClient to return empty rates
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(OpenExchangeRatesResponse.class))
                .thenReturn(Mono.just(new OpenExchangeRatesResponse("USD", Map.of(), Instant.now().getEpochSecond())));

        // Clear cache for this test
        Map<String, Map<String, BigDecimal>> emptyCache = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateCache", emptyCache);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            exchangeRateService.getExchangeRate("USD", "EUR")
        );
        
        // Verify WebClient was called
        verify(webClient, atLeastOnce()).get();
        
        // Verify currency service was called with the correct arguments
        verify(currencyService, atLeastOnce()).getCurrencyByCode("USD");
        verify(currencyService, atLeastOnce()).getCurrencyByCode("EUR");
        
        // Verify repository was not called since we're not using it in this flow
        verifyNoInteractions(exchangeRateRepository);
    }
}
