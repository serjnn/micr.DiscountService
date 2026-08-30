package com.serjnn.discountservice.mapper;

import com.serjnn.discountservice.dto.DiscountRequest;
import com.serjnn.discountservice.dto.DiscountResponse;
import com.serjnn.discountservice.model.DiscountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DiscountMapper {

    DiscountResponse toResponse(DiscountEntity entity);

    List<DiscountResponse> toResponseList(List<DiscountEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiscountEntity toEntity(DiscountRequest request);
}
