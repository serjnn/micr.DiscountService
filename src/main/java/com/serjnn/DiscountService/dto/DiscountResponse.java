package com.serjnn.DiscountService.dto;

import java.math.BigDecimal;

public record DiscountResponse(
        long productId,
        BigDecimal discount
) {
}
