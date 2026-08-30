package com.serjnn.DiscountService.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DiscountRequest(
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be greater than 0")
        Long productId,

        @NotNull(message = "Discount percentage is required")
        @DecimalMin(value = "0.00", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100.00%")
        BigDecimal discount
) {
}
