package com.serjnn.DiscountService.dto;

public record DiscountResponse(
        long productId,
        double discount
) {
}
