package com.serjnn.DiscountService.controller;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.dto.PageResponse;
import com.serjnn.DiscountService.service.DiscountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    public List<DiscountResponse> getAllDiscounts() {
        log.info("Received request to get all discounts");
        List<DiscountResponse> discounts = discountService.getAllDiscounts();
        log.info("Returning {} discounts", discounts.size());
        return discounts;
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<DiscountResponse>> getPagedDiscounts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.info("Received request to get discounts page: {}, size: {}", page, size);
        PageResponse<DiscountResponse> pagedDiscounts = discountService.getDiscounts(page, size);
        return ResponseEntity.ok(pagedDiscounts);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createOrUpdateDiscounts(
            @RequestBody @NotEmpty(message = "Discount requests list cannot be empty") List<@Valid DiscountRequest> discountRequests) {
        log.info("Received request to create/update {} discounts", discountRequests.size());
        discountService.addDiscounts(discountRequests);
        log.info("Successfully processed discount create/update request");
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateDiscount(
            @PathVariable @Positive(message = "Product ID must be positive") long productId,
            @RequestBody @Valid DiscountRequest discountRequest) {
        log.info("Received request to update discount for product id: {}", productId);
        DiscountRequest request = new DiscountRequest(productId, discountRequest.discount());
        discountService.addOrUpdateDiscount(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<DiscountResponse> getByProductId(
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

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteDiscount(
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
