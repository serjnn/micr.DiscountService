package com.serjnn.DiscountService.controller;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.service.DiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createOrUpdateDiscounts(@RequestBody List<DiscountRequest> discountRequests) {
        log.info("Received request to create/update {} discounts", discountRequests.size());
        discountService.addDiscounts(discountRequests);
        log.info("Successfully processed discount create/update request");
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateDiscount(
            @PathVariable long productId,
            @RequestBody DiscountRequest discountRequest) {
        log.info("Received request to update discount for product id: {}", productId);
        DiscountRequest request = new DiscountRequest(productId, discountRequest.discount());
        discountService.addOrUpdateDiscount(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<DiscountResponse> getByProductId(@PathVariable long productId) {
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
    public ResponseEntity<Void> deleteDiscount(@PathVariable long productId) {
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
