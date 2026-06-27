package com.serjnn.DiscountService.repository;

import com.serjnn.DiscountService.model.DiscountEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DiscountRepository extends ReactiveCrudRepository<DiscountEntity, Long> {
    Mono<DiscountEntity> findByProductId(long productId);
}
