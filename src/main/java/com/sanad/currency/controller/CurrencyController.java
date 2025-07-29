package com.sanad.currency.controller;

import com.sanad.currency.dto.CurrencyRequest;
import com.sanad.currency.dto.CurrencyResponse;
import com.sanad.currency.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Management", description = "APIs for managing currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new currency")
    public CurrencyResponse addCurrency(@Valid @RequestBody CurrencyRequest request) {
        return currencyService.addCurrency(request);
    }

    @GetMapping
    @Operation(summary = "Get all currencies")
    public List<CurrencyResponse> getAllCurrencies() {
        return currencyService.getAllCurrencies();
    }
}
