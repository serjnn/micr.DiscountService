package com.serjnn.DiscountService.service;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.kafka.KafkaSender;
import com.serjnn.DiscountService.model.DiscountEntity;
import com.serjnn.DiscountService.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final KafkaSender kafkaSender;

    @Value("${spring.kafka.topic.discount-changes}")
    private String discountChangesTopic;

    public void addDiscounts(List<DiscountRequest> requests) {
        log.info("Starting processing {} discount requests", requests.size());
        for (DiscountRequest request : requests) {
            long productId = request.productId();
            double newDiscount = request.discount();
            log.debug("Processing discount for product {}: {}", productId, newDiscount);

            Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);

            if (existingOpt.isPresent()) {
                DiscountEntity existing = existingOpt.get();
                double prevDiscount = existing.discount();

                log.info("Found existing discount for product {}: prev={}, new={}", productId, prevDiscount, newDiscount);

                DiscountEntity updated = new DiscountEntity(existing.id(), productId, newDiscount);
                discountRepository.update(updated);

                sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, prevDiscount));
                log.info("Updated discount for product {}: {} -> {}", productId, prevDiscount, newDiscount);
            } else {
                log.info("No existing discount for product {}. Adding new discount: {}", productId, newDiscount);
                DiscountEntity newEntity = new DiscountEntity(null, productId, newDiscount);
                discountRepository.save(newEntity);

                sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, 0.0));
                log.info("Added new discount for product {}: {}", productId, newDiscount);
            }
        }
        log.info("Finished processing discount requests");
    } // todo


    private void sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        log.debug("Sending discount change event to Kafka: {}", discountChangesDto);
        kafkaSender.sendNewDiscount(discountChangesTopic, discountChangesDto);
    }

    public Optional<DiscountResponse> findByProductId(long productId) {
        log.debug("Searching for discount for product id: {}", productId);
        return discountRepository.findByProductId(productId)
                .map(entity -> {
                    log.debug("Discount found for product id: {}", productId);
                    return new DiscountResponse(entity.productId(), entity.discount());
                });
    }

    public List<DiscountResponse> getAllDiscounts() {
        log.debug("Fetching all discounts from repository");
        List<DiscountResponse> discounts = discountRepository.findAll().stream()
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()))
                .toList();
        log.debug("Found {} discounts", discounts.size());
        return discounts;
    }
}
