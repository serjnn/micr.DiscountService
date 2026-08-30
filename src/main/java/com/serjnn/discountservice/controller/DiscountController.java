package com.serjnn.discountservice.controller;

import com.serjnn.discountservice.dto.DiscountRequest;
import com.serjnn.discountservice.dto.DiscountResponse;
import com.serjnn.discountservice.dto.PageResponse;
import com.serjnn.discountservice.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
@Tag(name = "Discounts", description = "Operations for managing product discounts and publishing change events")
public class DiscountController {

    private final DiscountService discountService;

    @Operation(summary = "Get all discounts", description = "Retrieves an unpaginated list of all active product discounts")
    @ApiResponse(responseCode = "200", description = "List of discounts retrieved successfully")
    @GetMapping
    public List<DiscountResponse> getAllDiscounts() {
        log.info("Received request to get all discounts");
        List<DiscountResponse> discounts = discountService.getAllDiscounts();
        log.info("Returning {} discounts", discounts.size());
        return discounts;
    }

    @Operation(summary = "Get paginated discounts", description = "Retrieves a paginated list of product discounts")
    @ApiResponse(responseCode = "200", description = "Page of discounts retrieved successfully")
    @GetMapping("/paged")
    public ResponseEntity<PageResponse<DiscountResponse>> getPagedDiscounts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1 to 100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Received request to get discounts page: {}, size: {}", page, size);
        PageResponse<DiscountResponse> pagedDiscounts = discountService.getDiscounts(page, size);
        return ResponseEntity.ok(pagedDiscounts);
    }

    @Operation(summary = "Batch create or update discounts", description = "Atomically creates or updates discounts in batch and broadcasts changes to Kafka")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Batch discounts processed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for one or more discount entries",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createOrUpdateDiscounts(
            @RequestBody @NotEmpty(message = "Discount requests list cannot be empty") List<@Valid DiscountRequest> discountRequests) {
        log.info("Received request to create/update {} discounts", discountRequests.size());
        discountService.addDiscounts(discountRequests);
        log.info("Successfully processed discount create/update request");
    }

    @Operation(summary = "Create or update discount for a single product", description = "Atomically creates or updates a product discount and broadcasts the change to Kafka")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Discount successfully updated or created"),
            @ApiResponse(responseCode = "400", description = "Invalid product ID or discount value",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateDiscount(
            @Parameter(description = "Product identifier", example = "101")
            @PathVariable @Positive(message = "Product ID must be positive") long productId,
            @RequestBody @Valid DiscountRequest discountRequest) {
        log.info("Received request to update discount for product id: {}", productId);
        DiscountRequest request = new DiscountRequest(productId, discountRequest.discount());
        discountService.addOrUpdateDiscount(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get discount by product ID", description = "Retrieves the currently configured discount for a given product ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Discount found"),
            @ApiResponse(responseCode = "404", description = "Discount not found for product ID",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{productId}")
    public ResponseEntity<DiscountResponse> getByProductId(
            @Parameter(description = "Product identifier", example = "101")
            @PathVariable @Positive(message = "Product ID must be positive") long productId) {
        log.info("Received request to get discount for product id: {}", productId);
        return discountService.findByProductId(productId)
                .map(response -> {
                    log.info("Discount found for product id: {}", productId);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    log.warn("Discount not found for product id: {}", productId);
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(summary = "Delete discount by product ID", description = "Deletes the configured discount for a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Discount deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Discount not found for product ID",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteDiscount(
            @Parameter(description = "Product identifier", example = "101")
            @PathVariable @Positive(message = "Product ID must be positive") long productId) {
        log.info("Received request to delete discount for product id: {}", productId);
        boolean deleted = discountService.deleteDiscount(productId);
        if (deleted) {
            log.info("Discount deleted for product id: {}", productId);
            return ResponseEntity.noContent().build();
        }
        log.warn("Cannot delete non-existent discount for product id: {}", productId);
        return ResponseEntity.notFound().build();
    }
}
