package com.mercadosolidario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DonationProperties {

    private final BigDecimal maximumAmount;

    public DonationProperties(
            @Value("${donations.maximum-amount:100000.00}") BigDecimal maximumAmount) {
        this.maximumAmount = maximumAmount;
    }

    public BigDecimal maximumAmount() {
        return maximumAmount;
    }
}
