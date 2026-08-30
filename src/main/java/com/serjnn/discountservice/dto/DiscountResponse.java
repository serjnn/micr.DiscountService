package com.serjnn.discountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response model representing a product discount")
public record DiscountResponse(
        @Schema(description = "Unique identifier of the product", example = "101")
        long productId,

        @Schema(description = "Current discount percentage", example = "15.50")
        BigDecimal discount
) {
}

