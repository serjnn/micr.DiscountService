package com.serjnn.DiscountService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating or updating a discount")
public record DiscountRequest(
    @Schema(description = "ID of the product to discount", example = "1")
    @NotNull(message = "Product ID must not be null")
    Long productId,

    @Schema(description = "Discount percentage value between 0.0 and 100.0", example = "15.0")
    @NotNull(message = "Discount value must not be null")
    @Min(value = 0, message = "Discount cannot be negative")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    Double discount
) {}
