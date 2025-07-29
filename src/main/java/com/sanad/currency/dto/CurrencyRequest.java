package com.sanad.currency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CurrencyRequest(
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be 3 characters long")
    @Pattern(regexp = "[A-Z]+", message = "Currency code must contain only uppercase letters")
    String code,
    
    @NotBlank(message = "Currency name is required")
    String name
) {}
