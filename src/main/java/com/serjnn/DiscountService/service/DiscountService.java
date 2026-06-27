package com.serjnn.DiscountService.service;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountMapper;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.kafka.KafkaSender;
import com.serjnn.DiscountService.model.DiscountEntity;
import com.serjnn.DiscountService.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final KafkaSender kafkaSender;
    private final DiscountMapper discountMapper;

    public Mono<Void> addDiscounts(List<DiscountRequest> discountRequests) {
        return Flux.fromIterable(discountRequests)
                .flatMap(request ->
                        discountRepository.findByProductId(request.productId())
                                .flatMap(existingEntity -> {
                                    double newDiscount = request.discount();
                                    double prevDiscount = existingEntity.getDiscount();
                                    sendDiscountChanges(new DiscountChangesDto(
                                            request.productId(),
                                            newDiscount,
                                            prevDiscount));

                                    existingEntity.setDiscount(newDiscount);
                                    return discountRepository.save(existingEntity);
                                })
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            DiscountEntity newEntity = discountMapper.toEntity(request);
                                            return discountRepository.save(newEntity)
                                                    .doOnNext(savedEntity -> sendDiscountChanges(
                                                            new DiscountChangesDto(
                                                                    savedEntity.getProductId(),
                                                                    savedEntity.getDiscount()
                                                            )
                                                    ));
                                        })
                                    )
                )
                .then();
    }

    private void sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        kafkaSender.sendNewDiscount("discountChangesTopic", discountChangesDto);
    }

    public Mono<DiscountResponse> findByProductId(long productId) {
        return discountRepository.findByProductId(productId)
                .map(discountMapper::toResponse);
    }

    public Flux<DiscountResponse> findAll() {
        return discountRepository.findAll()
                .map(discountMapper::toResponse);
    }
}
