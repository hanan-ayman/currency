package com.sanad.currency.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResponse(
    String baseCurrency,
    String targetCurrency,
    BigDecimal rate,
    Instant timestamp
) {}
