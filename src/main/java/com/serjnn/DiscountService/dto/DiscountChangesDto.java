package com.serjnn.DiscountService.dto;

public record DiscountChangesDto(
        long productId,
        double newDiscount,
        double prevDiscount
) {
}
