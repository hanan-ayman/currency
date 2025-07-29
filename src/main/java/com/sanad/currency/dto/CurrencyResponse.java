package com.sanad.currency.dto;

import java.time.Instant;

public record CurrencyResponse(
    Long id,
    String code,
    String name,
    Instant createdAt,
    Instant updatedAt
) {}
