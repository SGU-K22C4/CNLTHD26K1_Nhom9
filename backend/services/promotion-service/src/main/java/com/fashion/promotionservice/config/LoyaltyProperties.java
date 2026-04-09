package com.fashion.promotionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "loyalty")
@Getter
@Setter
public class LoyaltyProperties {

    private int pointToVnd = 100;

    private BigDecimal orderPointPercent = new BigDecimal("0.01");

    private int reviewBonusPoints = 10;
}
