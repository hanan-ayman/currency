package com.sanad.currency.mapper;

import com.sanad.currency.dto.CurrencyResponse;
import com.sanad.currency.model.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CurrencyMapper {
    CurrencyResponse toResponse(Currency currency);
}
