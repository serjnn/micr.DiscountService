package com.serjnn.DiscountService.controller;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
public class DiscountController {
y
    private final DiscountService discountService;

    @GetMapping
    public List<DiscountResponse> getAllDiscounts() {
        return discountService.getAllDiscounts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createOrUpdateDiscounts(@RequestBody List<DiscountRequest> discountRequests) {
        discountService.addDiscounts(discountRequests);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<DiscountResponse> getByProductId(@PathVariable long productId) {
        return discountService.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
