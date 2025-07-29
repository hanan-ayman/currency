CREATE TABLE IF NOT EXISTS exchange_rate (
    id BIGSERIAL PRIMARY KEY,
    base_currency_id BIGINT NOT NULL,
    target_currency_id BIGINT NOT NULL,
    rate DECIMAL(20, 6) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_base_currency FOREIGN KEY (base_currency_id) REFERENCES currency(id),
    CONSTRAINT fk_target_currency FOREIGN KEY (target_currency_id) REFERENCES currency(id),
    CONSTRAINT unique_currency_pair UNIQUE (base_currency_id, target_currency_id, timestamp)
);

CREATE INDEX idx_exchange_rate_timestamp ON exchange_rate(timestamp);
CREATE INDEX idx_exchange_rate_currencies ON exchange_rate(base_currency_id, target_currency_id);