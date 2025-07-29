package com.sanad.currency.util;

import com.sanad.currency.model.Currency;
import com.sanad.currency.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

public class TestUtil {
    
    public static Currency createTestCurrency(String code, String name) {
        Currency currency = new Currency();
        currency.setCode(code);
        currency.setName(name);
        return currency;
    }
    
    public static ExchangeRate createTestExchangeRate(Currency base, Currency target, BigDecimal rate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setBaseCurrency(base);
        exchangeRate.setTargetCurrency(target);
        exchangeRate.setRate(rate);
        exchangeRate.setTimestamp(Instant.now());
        return exchangeRate;
    }
    
    public static String asJsonString(final Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
