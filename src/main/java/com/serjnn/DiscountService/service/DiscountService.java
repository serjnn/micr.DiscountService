package com.serjnn.DiscountService.service;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.kafka.KafkaSender;
import com.serjnn.DiscountService.model.DiscountEntity;
import com.serjnn.DiscountService.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final KafkaSender kafkaSender;

    public void addDiscounts(List<DiscountRequest> requests) {
        requests.forEach(request -> {
            discountRepository.findByProductId(request.productId()) // todo !!!
                    .ifPresentOrElse(
                            existing -> {
                                double newDiscount = request.discount();
                                double prevDiscount = existing.discount();

                                sendDiscountChanges(new DiscountChangesDto(
                                        request.productId(),
                                        newDiscount,
                                        prevDiscount));

                                DiscountEntity updated = new DiscountEntity(
                                        existing.id(),
                                        existing.productId(),
                                        newDiscount);
                                discountRepository.update(updated);
                                log.info("Updated discount for product {}: {} -> {}", 
                                        request.productId(), prevDiscount, newDiscount);
                            },
                            () -> {
                                DiscountEntity newEntity = new DiscountEntity(
                                        null, 
                                        request.productId(), 
                                        request.discount());
                                discountRepository.save(newEntity);
                                sendDiscountChanges(new DiscountChangesDto(
                                        request.productId(),
                                        request.discount()
                                ));
                                log.info("Added new discount for product {}: {}", 
                                        request.productId(), request.discount());
                            }
                    );
        });
    }

    private void sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        kafkaSender.sendNewDiscount("discountChangesTopic", discountChangesDto); // todo
    }

    public Optional<DiscountResponse> findByProductId(long productId) {
        return discountRepository.findByProductId(productId)
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()));
    }

    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()// todo !!!
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()))
                .toList();
    }
}
