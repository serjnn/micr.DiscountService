package com.serjnn.DiscountService.dto;

import java.math.BigDecimal;

public record DiscountRequest(
        long productId,
        BigDecimal discount
) {
}
