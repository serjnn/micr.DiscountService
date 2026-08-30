package com.serjnn.DiscountService.service;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.dto.PageResponse;
import com.serjnn.DiscountService.kafka.KafkaSender;
import com.serjnn.DiscountService.model.DiscountEntity;
import com.serjnn.DiscountService.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final KafkaSender kafkaSender;

    @Transactional
    public void addDiscounts(List<DiscountRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.info("Received empty or null discount request list. Nothing to process.");
            return;
        }

        log.info("Starting processing {} discount requests", requests.size());

        // Deduplicate requests preserving the latest value per product ID
        Map<Long, DiscountRequest> deduplicatedRequests = new LinkedHashMap<>();
        for (DiscountRequest request : requests) {
            deduplicatedRequests.put(request.productId(), request);
        }
        List<DiscountRequest> uniqueRequests = List.copyOf(deduplicatedRequests.values());

        // Batch fetch existing discounts to eliminate N+1 queries
        Map<Long, BigDecimal> existingDiscounts = discountRepository.findByProductIds(deduplicatedRequests.keySet())
                .stream()
                .collect(Collectors.toMap(DiscountEntity::productId, DiscountEntity::discount));

        // Atomic batch upsert
        discountRepository.batchUpsert(uniqueRequests);

        // Publish events for each updated/created discount
        for (DiscountRequest request : uniqueRequests) {
            long productId = request.productId();
            BigDecimal newDiscount = request.discount();
            BigDecimal prevDiscount = existingDiscounts.getOrDefault(productId, BigDecimal.ZERO);

            log.info("Processed discount for product {}: prev={}, new={}", productId, prevDiscount, newDiscount);
            sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, prevDiscount));
        }

        log.info("Finished processing {} discount requests", uniqueRequests.size());
    }

    @Transactional
    public void addOrUpdateDiscount(DiscountRequest request) {
        long productId = request.productId();
        BigDecimal newDiscount = request.discount();

        Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);
        BigDecimal prevDiscount = existingOpt.map(DiscountEntity::discount).orElse(BigDecimal.ZERO);

        discountRepository.upsert(productId, newDiscount);
        log.info("Processed single discount for product {}: prev={}, new={}", productId, prevDiscount, newDiscount);

        sendDiscountChanges(new DiscountChangesDto(productId, newDiscount, prevDiscount));
    }

    @Transactional
    public boolean deleteDiscount(long productId) {
        log.info("Deleting discount for product {}", productId);
        Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);
        if (existingOpt.isEmpty()) {
            log.warn("Cannot delete non-existent discount for product {}", productId);
            return false;
        }

        discountRepository.deleteByProductId(productId);
        log.info("Successfully deleted discount for product {}", productId);
        return true;
    }

    private void sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        log.debug("Sending discount change event to Kafka: {}", discountChangesDto);
        kafkaSender.sendDiscountChanges(discountChangesDto);
    }

    @Transactional(readOnly = true)
    public Optional<DiscountResponse> findByProductId(long productId) {
        log.debug("Searching for discount for product id: {}", productId);
        return discountRepository.findByProductId(productId)
                .map(entity -> {
                    log.debug("Discount found for product id: {}", productId);
                    return new DiscountResponse(entity.productId(), entity.discount());
                });
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        log.debug("Fetching all discounts from repository");
        List<DiscountResponse> discounts = discountRepository.findAll().stream()
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()))
                .toList();
        log.debug("Found {} discounts", discounts.size());
        return discounts;
    }

    @Transactional(readOnly = true)
    public PageResponse<DiscountResponse> getDiscounts(int page, int size) {
        log.debug("Fetching discounts page {} size {}", page, size);
        List<DiscountResponse> content = discountRepository.findAll(page, size).stream()
                .map(entity -> new DiscountResponse(entity.productId(), entity.discount()))
                .toList();
        long total = discountRepository.count();
        return PageResponse.of(content, page, size, total);
    }
}
