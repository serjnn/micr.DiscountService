package com.serjnn.DiscountService.dto;


public record DiscountRequest(
        long productId,
        double discount
) {
}
