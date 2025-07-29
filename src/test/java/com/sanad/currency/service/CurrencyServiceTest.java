package com.sanad.currency.service;

import com.sanad.currency.dto.CurrencyRequest;
import com.sanad.currency.dto.CurrencyResponse;
import com.sanad.currency.exception.DuplicateResourceException;
import com.sanad.currency.mapper.CurrencyMapper;
import com.sanad.currency.model.Currency;
import com.sanad.currency.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyMapper currencyMapper;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency currency;
    private CurrencyRequest request;
    private CurrencyResponse response;

    @BeforeEach
    void setUp() {
        currency = new Currency();
        currency.setId(1L);
        currency.setCode("USD");
        currency.setName("US Dollar");

        request = new CurrencyRequest("USD", "US Dollar");
        response = new CurrencyResponse(1L, "USD", "US Dollar", null, null);
    }

    @Test
    void addCurrency_ShouldReturnSavedCurrency() {
        // Arrange
        when(currencyRepository.existsByCode(anyString())).thenReturn(false);
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);
        when(currencyMapper.toResponse(any(Currency.class))).thenReturn(response);

        // Act
        CurrencyResponse result = currencyService.addCurrency(request);

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.code());
        verify(currencyRepository, times(1)).existsByCode("USD");
        verify(currencyRepository, times(1)).save(any(Currency.class));
    }

    @Test
    void addCurrency_WhenCurrencyExists_ShouldThrowException() {
        // Arrange
        when(currencyRepository.existsByCode(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> currencyService.addCurrency(request));
        verify(currencyRepository, never()).save(any(Currency.class));
    }

    @Test
    void getAllCurrencies_ShouldReturnListOfCurrencies() {
        // Arrange
        when(currencyRepository.findAll()).thenReturn(List.of(currency));
        when(currencyMapper.toResponse(any(Currency.class))).thenReturn(response);

        // Act
        var result = currencyService.getAllCurrencies();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("USD", result.get(0).code());
    }

    @Test
    void getCurrencyByCode_WhenCurrencyExists_ShouldReturnCurrency() {
        // Arrange
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(currency));

        // Act
        var result = currencyService.getCurrencyByCode("USD");

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.getCode());
        verify(currencyRepository).findByCode("USD");
    }

    @Test
    void getCurrencyByCode_WhenCurrencyNotExists_ShouldThrowException() {
        // Arrange
        String invalidCode = "INVALID";
        when(currencyRepository.findByCode(invalidCode)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(
            RuntimeException.class,
            () -> currencyService.getCurrencyByCode(invalidCode)
        );
        
        assertEquals("Currency not found with code: " + invalidCode, exception.getMessage());
        verify(currencyRepository).findByCode(invalidCode);
    }
}
