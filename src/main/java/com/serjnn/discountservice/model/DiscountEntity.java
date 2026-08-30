package com.serjnn.discountservice.model;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountEntity(
        Long id,
        long productId,
        BigDecimal discount,
        Instant createdAt,
        Instant updatedAt
) {
}

