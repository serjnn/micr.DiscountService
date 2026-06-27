package com.serjnn.DiscountService.dto;

import com.serjnn.DiscountService.model.DiscountEntity;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

    public DiscountResponse toResponse(DiscountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new DiscountResponse(
            entity.getId(),
            entity.getProductId(),
            entity.getDiscount()
        );
    }

    public DiscountEntity toEntity(DiscountRequest request) {
        if (request == null) {
            return null;
        }
        DiscountEntity entity = new DiscountEntity();
        entity.setProductId(request.productId());
        entity.setDiscount(request.discount());
        return entity;
    }
}
