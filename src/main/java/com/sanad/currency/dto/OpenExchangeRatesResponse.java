package com.sanad.currency.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

public record OpenExchangeRatesResponse(
        @JsonProperty("base") String base,
        @JsonProperty("rates") Map<String, BigDecimal> rates,
        @JsonProperty("timestamp") Long timestamp
) {}
