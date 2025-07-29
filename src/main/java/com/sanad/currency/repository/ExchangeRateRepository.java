package com.sanad.currency.repository;

import com.sanad.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    @Query("SELECT er FROM ExchangeRate er " +
           "WHERE er.baseCurrency.code = :baseCurrency " +
           "AND er.targetCurrency.code = :targetCurrency " +
           "ORDER BY er.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("baseCurrency") String baseCurrency, 
                                        @Param("targetCurrency") String targetCurrency);
}
