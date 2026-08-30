package com.serjnn.DiscountService.exception;

public class DiscountNotFoundException extends RuntimeException {

    private final long productId;

    public DiscountNotFoundException(long productId) {
        super("Discount not found for product ID: " + productId);
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
