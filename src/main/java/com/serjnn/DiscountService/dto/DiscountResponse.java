package com.serjnn.DiscountService.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Discount details response object")
public record DiscountResponse(
    @Schema(description = "Unique ID of the discount entry", example = "1")
    Long id,

    @Schema(description = "ID of the product", example = "1")
    Long productId,

    @Schema(description = "Discount percentage", example = "15.0")
    Double discount
) {}
