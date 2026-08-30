package com.serjnn.discountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Kafka event payload published when a product discount is created or updated")
public record DiscountChangesDto(
        @Schema(description = "Unique identifier of the product", example = "101")
        long productId,

        @Schema(description = "Newly applied discount percentage", example = "20.00")
        BigDecimal newDiscount,

        @Schema(description = "Previously active discount percentage (0.00 if newly created)", example = "15.50")
        BigDecimal prevDiscount
) {
}

