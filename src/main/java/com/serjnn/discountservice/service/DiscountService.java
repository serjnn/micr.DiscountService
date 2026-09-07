package com.serjnn.discountservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serjnn.discountservice.dto.DiscountChangesDto;
import com.serjnn.discountservice.dto.DiscountRequest;
import com.serjnn.discountservice.dto.DiscountResponse;
import com.serjnn.discountservice.dto.PageResponse;
import com.serjnn.discountservice.mapper.DiscountMapper;
import com.serjnn.discountservice.model.DiscountEntity;
import com.serjnn.discountservice.outbox.OutboxEvent;
import com.serjnn.discountservice.outbox.OutboxRepository;
import com.serjnn.discountservice.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final OutboxRepository outboxRepository;
    private final DiscountMapper discountMapper;
    private final ObjectMapper objectMapper;

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

        // Prepare transactional outbox events
        List<OutboxEvent> outboxEvents = new ArrayList<>();
        for (DiscountRequest request : uniqueRequests) {
            long productId = request.productId();
            BigDecimal newDiscount = request.discount();
            BigDecimal prevDiscount = existingDiscounts.getOrDefault(productId, BigDecimal.ZERO);

            log.info("Processed discount for product {}: prev={}, new={}", productId, prevDiscount, newDiscount);
            DiscountChangesDto changesDto = new DiscountChangesDto(productId, newDiscount, prevDiscount);
            outboxEvents.add(createOutboxEvent(changesDto));
        }

        outboxRepository.saveBatch(outboxEvents);
        log.info("Finished processing {} discount requests and saved {} outbox events", uniqueRequests.size(), outboxEvents.size());
    }

    @Transactional
    public void addOrUpdateDiscount(DiscountRequest request) {
        long productId = request.productId();
        BigDecimal newDiscount = request.discount();

        Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);
        BigDecimal prevDiscount = existingOpt.map(DiscountEntity::discount).orElse(BigDecimal.ZERO);

        discountRepository.upsert(productId, newDiscount);
        log.info("Processed single discount for product {}: prev={}, new={}", productId, prevDiscount, newDiscount);

        DiscountChangesDto changesDto = new DiscountChangesDto(productId, newDiscount, prevDiscount);
        outboxRepository.save(createOutboxEvent(changesDto));
    }

    @Transactional
    public boolean deleteDiscount(long productId) {
        log.info("Deleting discount for product {}", productId);
        Optional<DiscountEntity> existingOpt = discountRepository.findByProductId(productId);
        if (existingOpt.isEmpty()) {
            log.warn("Cannot delete non-existent discount for product {}", productId);
            return false;
        }

        BigDecimal prevDiscount = existingOpt.get().discount();
        discountRepository.deleteByProductId(productId);
        log.info("Successfully deleted discount for product {}: prev={}", productId, prevDiscount);

        DiscountChangesDto changesDto = new DiscountChangesDto(productId, BigDecimal.ZERO, prevDiscount);
        outboxRepository.save(createOutboxEvent(changesDto));
        return true;
    }

    private OutboxEvent createOutboxEvent(DiscountChangesDto changesDto) {
        try {
            String payload = objectMapper.writeValueAsString(changesDto);
            return OutboxEvent.pending(
                    "PRODUCT_DISCOUNT",
                    String.valueOf(changesDto.productId()),
                    "DISCOUNT_CHANGED",
                    payload
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize discount changes to JSON", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DiscountResponse> findByProductId(long productId) {
        log.debug("Searching for discount for product id: {}", productId);
        return discountRepository.findByProductId(productId)
                .map(discountMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        log.debug("Fetching all discounts from repository");
        List<DiscountResponse> discounts = discountMapper.toResponseList(discountRepository.findAll());
        log.debug("Found {} discounts", discounts.size());
        return discounts;
    }

    @Transactional(readOnly = true)
    public PageResponse<DiscountResponse> getDiscounts(int page, int size) {
        log.debug("Fetching discounts page {} size {}", page, size);
        List<DiscountResponse> content = discountMapper.toResponseList(discountRepository.findAll(page, size));
        long total = discountRepository.count();
        return PageResponse.of(content, page, size, total);
    }
}
