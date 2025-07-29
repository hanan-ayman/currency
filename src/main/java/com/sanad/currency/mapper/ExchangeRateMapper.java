package com.sanad.currency.mapper;

import com.sanad.currency.dto.ExchangeRateResponse;
import com.sanad.currency.model.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExchangeRateMapper {
    @Mapping(source = "baseCurrency.code", target = "baseCurrency")
    @Mapping(source = "targetCurrency.code", target = "targetCurrency")
    ExchangeRateResponse toResponse(ExchangeRate exchangeRate);
}
