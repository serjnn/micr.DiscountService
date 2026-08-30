package com.serjnn.DiscountService.model;

import java.math.BigDecimal;

public record DiscountEntity(
        Long id,
        long productId,
        BigDecimal discount
) {
}
