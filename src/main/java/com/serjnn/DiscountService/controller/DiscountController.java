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
}
