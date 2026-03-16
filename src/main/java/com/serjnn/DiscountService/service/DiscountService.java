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
        for (DiscountRequest request : requests) {
            long productId = request.productId();
            double newDiscount = request.discount();

            Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);

            if (existingOpt.isPresent()) {
                DiscountEntity existing = existingOpt.get();
                double prevDiscount = existing.discount();

                DiscountEntity updated = new DiscountEntity(existing.id(), productId, newDiscount);
                discountRepository.update(updated);

                sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, prevDiscount));
                log.info("Updated discount for product {}: {} -> {}", productId, prevDiscount, newDiscount);
            } else {
                DiscountEntity newEntity = new DiscountEntity(null, productId, newDiscount);
                discountRepository.save(newEntity);

                sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, 0.0));
                log.info("Added new discount for product {}: {}", productId, newDiscount);
            }
        }
    }


    private void sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        kafkaSender.sendNewDiscount("discountChangesTopic", discountChangesDto);
    }

    public Optional<DiscountResponse> findByProductId(long productId) {
        return discountRepository.findByProductId(productId)
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()));
    }

    public List<DiscountResponse> getAllDiscounts() {
        return discountRepository.findAll().stream()
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()))
                .toList();
    }
}
