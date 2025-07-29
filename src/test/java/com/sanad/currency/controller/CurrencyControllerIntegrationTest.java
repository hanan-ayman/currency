package com.sanad.currency.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanad.currency.dto.CurrencyRequest;
import com.sanad.currency.model.Currency;
import com.sanad.currency.repository.CurrencyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.sanad.currency.config.TestConfig;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sanad.currency.util.TestUtil;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class CurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        // Ensure the database is clean before each test
        currencyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        currencyRepository.deleteAll();
    }

    @Test
    void addCurrency_ShouldReturnCreatedCurrency() throws Exception {
        // Given
        CurrencyRequest request = new CurrencyRequest("USD", "US Dollar");

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.asJsonString(request)))
                .andDo(print())  // This will print the request and response for debugging
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.name").value("US Dollar"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        // Verify the currency was saved to the database
        List<Currency> currencies = currencyRepository.findAll();
        assertFalse(currencies.isEmpty(), "Currency should be saved to the database");
        assertEquals("USD", currencies.get(0).getCode(), "Currency code should match");
        assertEquals("US Dollar", currencies.get(0).getName(), "Currency name should match");
    }

    @Test
    void addCurrency_WhenCurrencyExists_ShouldReturnConflict() throws Exception {
        // Given
        Currency existingCurrency = new Currency();
        existingCurrency.setCode("USD");
        existingCurrency.setName("US Dollar");
        currencyRepository.save(existingCurrency);

        CurrencyRequest request = new CurrencyRequest("USD", "US Dollar");

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Currency with code USD already exists"));

        List<Currency> currencies = currencyRepository.findAll();
        assertEquals(1, currencies.size(), "Should not create duplicate currency");
    }


    @Test
    void addCurrency_WithEmptyRequest_ShouldReturnBadRequest() throws Exception {
        // Given - empty request body
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/currencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCurrency_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CurrencyRequest request = new CurrencyRequest("", "");

        mockMvc.perform(post("/api/v1/currencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrencies_WhenNoCurrenciesExist_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/currencies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
