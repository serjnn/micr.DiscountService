package com.serjnn.DiscountService.dto;

import java.math.BigDecimal;

public record DiscountChangesDto(
        long productId,
        BigDecimal newDiscount,
        BigDecimal prevDiscount
) {
}
