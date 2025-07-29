package com.sanad.currency.controller;

import com.sanad.currency.config.TestConfig;
import com.sanad.currency.dto.ExchangeRateResponse;
import com.sanad.currency.model.Currency;
import com.sanad.currency.model.ExchangeRate;
import com.sanad.currency.repository.CurrencyRepository;
import com.sanad.currency.repository.ExchangeRateRepository;
import com.sanad.currency.service.ExchangeRateService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class ExchangeRateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    
    @SpyBean
    private ExchangeRateService exchangeRateService;
    


    private Currency usdCurrency;
    private Currency eurCurrency;
    private ExchangeRate exchangeRate;

    @BeforeEach
    void setUp() {
        // Clear data before each test
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();

        // Setup test data
        usdCurrency = new Currency();
        usdCurrency.setCode("USD");
        usdCurrency.setName("US Dollar");
        usdCurrency = currencyRepository.save(usdCurrency);

        eurCurrency = new Currency();
        eurCurrency.setCode("EUR");
        eurCurrency.setName("Euro");
        eurCurrency = currencyRepository.save(eurCurrency);

        exchangeRate = new ExchangeRate();
        exchangeRate.setBaseCurrency(usdCurrency);
        exchangeRate.setTargetCurrency(eurCurrency);
        exchangeRate.setRate(new BigDecimal("0.85"));
        exchangeRate.setTimestamp(Instant.now());
        exchangeRate = exchangeRateRepository.save(exchangeRate);
        
        // Verify data was saved
        assertNotNull(exchangeRate.getId(), "Exchange rate should have an ID after save");
        assertEquals(1, exchangeRateRepository.count(), "Should have exactly one exchange rate");
    }

    @Test
    void getExchangeRate_ShouldReturnExchangeRate() throws Exception {
        // Given - Data is already set up in setUp()
        ExchangeRateResponse mockResponse = new ExchangeRateResponse("USD", "EUR", new BigDecimal("0.85"), Instant.now());
        doReturn(mockResponse).when(exchangeRateService).getExchangeRate("USD", "EUR");
        
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("base", "USD")
                .param("target", "EUR")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.targetCurrency").value("EUR"))
                .andExpect(jsonPath("$.rate").value(0.85))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getExchangeRate_WhenBaseCurrencyNotExist_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("base", "XXX")
                .param("target", "EUR")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getExchangeRate_WhenTargetCurrencyNotExist_ShouldReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("base", "USD")
                .param("target", "XXX")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getExchangeRate_WithInvalidCurrencyCode_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("base", "US")
                .param("target", "EU")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExchangeRate_WithMissingBaseParameter_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("target", "EUR")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getExchangeRate_WithMissingTargetParameter_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .param("base", "USD")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getExchangeRate_WithMissingAllParameters_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exchange-rates")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data after each test
        try {
            exchangeRateRepository.deleteAll();
            currencyRepository.deleteAll();
        } catch (Exception e) {
            // Log the exception but don't fail the test
            System.err.println("Error during test teardown: " + e.getMessage());
        }
    }
}
