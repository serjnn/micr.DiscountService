package com.serjnn.DiscountService.dto;

public record DiscountChangesDto(
        long productId,
        double newDiscount,
        Double prevDiscount
) {
    public DiscountChangesDto(long productId, double newDiscount) {
        this(productId, newDiscount, null);
    }
}
