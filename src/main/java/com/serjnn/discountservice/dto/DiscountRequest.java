package com.serjnn.discountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Request model for creating or updating a product discount")
public record DiscountRequest(
        @Schema(description = "Unique identifier of the product", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be greater than 0")
        Long productId,

        @Schema(description = "Discount percentage (0.00 to 100.00)", example = "15.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Discount percentage is required")
        @DecimalMin(value = "0.00", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100.00%")
        BigDecimal discount
) {
}

