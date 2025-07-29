package com.sanad.currency.service;

import com.sanad.currency.dto.CurrencyRequest;
import com.sanad.currency.dto.CurrencyResponse;
import com.sanad.currency.exception.DuplicateResourceException;
import com.sanad.currency.exception.ResourceNotFoundException;
import com.sanad.currency.mapper.CurrencyMapper;
import com.sanad.currency.model.Currency;
import com.sanad.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    @Transactional
    public CurrencyResponse addCurrency(CurrencyRequest request) {
        // Check if currency already exists
        if (currencyRepository.existsByCode(request.code().toUpperCase())) {
            throw new DuplicateResourceException("Currency with code " + request.code() + " already exists");
        }

        // Create and save new currency
        Currency currency = new Currency();
        currency.setCode(request.code().toUpperCase());
        currency.setName(request.name());
        
        Currency savedCurrency = currencyRepository.save(currency);
        return currencyMapper.toResponse(savedCurrency);
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllCurrencies() {
        return currencyRepository.findAll().stream()
                .map(currencyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found with code: " + code));
    }
}
