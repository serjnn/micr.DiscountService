package com.serjnn.DiscountService.controller;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.service.DiscountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
@Tag(name = "Discount API", description = "Endpoints for managing product discounts")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    @Operation(summary = "Get all discounts", description = "Retrieve a list of all active product discounts")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved discounts list")
    public Flux<DiscountResponse> getAll() {
        log.info("Request received: Get all discounts");
        return discountService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add discounts", description = "Add or update discounts for multiple products")
    @ApiResponse(responseCode = "201", description = "Discounts successfully created/updated")
    @ApiResponse(responseCode = "400", description = "Invalid input payload")
    public Mono<Void> add(@Valid @RequestBody List<DiscountRequest> discountRequests) {
        log.info("Request received: Add discounts: {}", discountRequests);
        return discountService.addDiscounts(discountRequests);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get discount by product ID", description = "Retrieve discount details for a specific product ID")
    @ApiResponse(responseCode = "200", description = "Discount found")
    @ApiResponse(responseCode = "404", description = "Discount not found for the given product ID")
    public Mono<ResponseEntity<DiscountResponse>> byProductId(@PathVariable("productId") long productId) {
        log.info("Request received: Get discount by product ID: {}", productId);
        return discountService.findByProductId(productId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
