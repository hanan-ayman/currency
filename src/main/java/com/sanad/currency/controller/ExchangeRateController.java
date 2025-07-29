package com.sanad.currency.controller;

import com.sanad.currency.dto.ExchangeRateResponse;
import com.sanad.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rate Management", description = "APIs for managing exchange rates")
@Validated
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "Get exchange rate between two currencies")
    public ExchangeRateResponse getExchangeRate(
            @RequestParam @Pattern(regexp = "[A-Z]{3}", message = "Base currency code must be 3 uppercase letters") 
            @Parameter(description = "Base currency code (e.g., USD, EUR)") String base,
            
            @RequestParam @Pattern(regexp = "[A-Z]{3}", message = "Target currency code must be 3 uppercase letters") 
            @Parameter(description = "Target currency code (e.g., USD, EUR)") String target) {
        
        return exchangeRateService.getExchangeRate(base, target);
    }
}
