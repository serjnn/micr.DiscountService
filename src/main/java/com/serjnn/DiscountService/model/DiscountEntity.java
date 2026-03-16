package com.serjnn.DiscountService.model;


public record DiscountEntity(
        Long id,
        long productId,
        double discount
) {
}
