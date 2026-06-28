package com.serjnn.DiscountService.service;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountMapper;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.kafka.producer.KafkaSender;
import com.serjnn.DiscountService.model.DiscountEntity;
import com.serjnn.DiscountService.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final KafkaSender kafkaSender;
    private final DiscountMapper discountMapper;

    public Mono<Void> addDiscounts(List<DiscountRequest> discountRequests) {
        log.info("Processing {} discount requests", discountRequests.size());
        return Flux.fromIterable(discountRequests)
                .flatMap(request ->
                        discountRepository.findByProductId(request.productId())
                                .flatMap(existingEntity -> {
                                    double newDiscount = request.discount();
                                    double prevDiscount = existingEntity.getDiscount();
                                    existingEntity.setDiscount(newDiscount);
                                    return discountRepository.save(existingEntity)
                                            .flatMap(savedEntity -> sendDiscountChanges(new DiscountChangesDto(
                                                    request.productId(),
                                                    newDiscount,
                                                    prevDiscount
                                            )).thenReturn(savedEntity));
                                })
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            DiscountEntity newEntity = discountMapper.toEntity(request);
                                            return discountRepository.save(newEntity)
                                                    .flatMap(savedEntity -> sendDiscountChanges(new DiscountChangesDto(
                                                            savedEntity.getProductId(),
                                                            savedEntity.getDiscount()
                                                    )).thenReturn(savedEntity));
                                        })
                                    )
                )
                .then();
    }

    private Mono<Void> sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        return kafkaSender.sendNewDiscount("discountChangesTopic", discountChangesDto);
    }

    public Mono<DiscountResponse> findByProductId(long productId) {
        log.info("Finding discount by product ID: {}", productId);
        return discountRepository.findByProductId(productId)
                .map(discountMapper::toResponse);
    }

    public Flux<DiscountResponse> findAll() {
        log.info("Finding all discounts");
        return discountRepository.findAll()
                .map(discountMapper::toResponse);
    }
}
