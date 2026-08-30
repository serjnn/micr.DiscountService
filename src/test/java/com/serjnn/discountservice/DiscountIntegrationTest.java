package com.serjnn.discountservice;

import com.serjnn.discountservice.dto.DiscountChangesDto;
import com.serjnn.discountservice.dto.DiscountRequest;
import com.serjnn.discountservice.dto.DiscountResponse;
import com.serjnn.discountservice.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DiscountIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final BlockingQueue<DiscountChangesDto> kafkaMessages = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE discount_entity, outbox_events RESTART IDENTITY CASCADE");
        kafkaMessages.clear();
    }

    @KafkaListener(topics = "${spring.kafka.topic.discount-changes}", groupId = "test-group")
    public void listen(DiscountChangesDto message) {
        kafkaMessages.add(message);
    }

    @Test
    void shouldCreateAndThenUpdateDiscount() throws InterruptedException {
        // 1. Add new discount
        DiscountRequest request = new DiscountRequest(101L, new BigDecimal("15.50"));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/discounts", List.of(request), Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Verify it's in DB
        ResponseEntity<DiscountResponse> getResponse = restTemplate.getForEntity("/api/v1/discounts/101", DiscountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().productId()).isEqualTo(101L);
        assertThat(getResponse.getBody().discount()).isEqualByComparingTo(new BigDecimal("15.50"));

        // 3. Verify Kafka message for NEW discount
        DiscountChangesDto firstMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(firstMessage).isNotNull();
        assertThat(firstMessage.productId()).isEqualTo(101L);
        assertThat(firstMessage.newDiscount()).isEqualByComparingTo(new BigDecimal("15.50"));
        assertThat(firstMessage.prevDiscount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 4. Update existing discount
        DiscountRequest updateRequest = new DiscountRequest(101L, new BigDecimal("20.00"));
        restTemplate.postForEntity("/api/v1/discounts", List.of(updateRequest), Void.class);

        // 5. Verify updated in DB
        ResponseEntity<DiscountResponse> updatedGetResponse = restTemplate.getForEntity("/api/v1/discounts/101", DiscountResponse.class);
        assertThat(updatedGetResponse.getBody()).isNotNull();
        assertThat(updatedGetResponse.getBody().discount()).isEqualByComparingTo(new BigDecimal("20.00"));

        // 6. Verify Kafka message for UPDATED discount
        DiscountChangesDto secondMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(secondMessage).isNotNull();
        assertThat(secondMessage.productId()).isEqualTo(101L);
        assertThat(secondMessage.newDiscount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(secondMessage.prevDiscount()).isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void shouldGetAllDiscounts() {
        DiscountRequest req1 = new DiscountRequest(201L, new BigDecimal("10.00"));
        DiscountRequest req2 = new DiscountRequest(202L, new BigDecimal("20.00"));
        restTemplate.postForEntity("/api/v1/discounts", List.of(req1, req2), Void.class);

        ResponseEntity<List<DiscountResponse>> response = restTemplate.exchange(
                "/api/v1/discounts",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DiscountResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<DiscountResponse> discounts = response.getBody();
        assertThat(discounts).extracting(DiscountResponse::productId).contains(201L, 202L);
    }

    @Test
    void shouldReturn404WhenDiscountNotFound() {
        ResponseEntity<DiscountResponse> response = restTemplate.getForEntity("/api/v1/discounts/99999", DiscountResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateDiscountViaPutAndEmitKafkaEvent() throws InterruptedException {
        DiscountRequest putRequest = new DiscountRequest(301L, new BigDecimal("35.00"));
        restTemplate.put("/api/v1/discounts/301", putRequest);

        ResponseEntity<DiscountResponse> getResponse = restTemplate.getForEntity("/api/v1/discounts/301", DiscountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().productId()).isEqualTo(301L);
        assertThat(getResponse.getBody().discount()).isEqualByComparingTo(new BigDecimal("35.00"));

        DiscountChangesDto kafkaMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(kafkaMessage).isNotNull();
        assertThat(kafkaMessage.productId()).isEqualTo(301L);
        assertThat(kafkaMessage.newDiscount()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(kafkaMessage.prevDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldDeleteDiscountAndReturn404WhenNotFound() {
        DiscountRequest request = new DiscountRequest(401L, new BigDecimal("12.50"));
        restTemplate.postForEntity("/api/v1/discounts", List.of(request), Void.class);

        // Delete existing discount
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/discounts/401",
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deleted
        ResponseEntity<DiscountResponse> getResponse = restTemplate.getForEntity("/api/v1/discounts/401", DiscountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Deleting non-existent should return 404
        ResponseEntity<Void> deleteNonExistentResponse = restTemplate.exchange(
                "/api/v1/discounts/401",
                HttpMethod.DELETE,
                null,
                Void.class
        );
        assertThat(deleteNonExistentResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldHandleBatchWithDuplicateProductIdsGracefully() throws InterruptedException {
        DiscountRequest req1 = new DiscountRequest(501L, new BigDecimal("10.00"));
        DiscountRequest req2 = new DiscountRequest(501L, new BigDecimal("25.00"));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/discounts", List.of(req1, req2), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<DiscountResponse> getResponse = restTemplate.getForEntity("/api/v1/discounts/501", DiscountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().discount()).isEqualByComparingTo(new BigDecimal("25.00"));

        DiscountChangesDto kafkaMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(kafkaMessage).isNotNull();
        assertThat(kafkaMessage.productId()).isEqualTo(501L);
        assertThat(kafkaMessage.newDiscount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void shouldReturn400WhenValidationFailsOnInvalidDiscount() {
        // Discount > 100%
        DiscountRequest invalidReq = new DiscountRequest(601L, new BigDecimal("150.00"));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/discounts", List.of(invalidReq), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation Failed");
    }

    @Test
    void shouldReturn400WhenBatchListIsEmpty() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/discounts", List.of(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldGetPagedDiscountsCorrectly() {
        for (int i = 1; i <= 5; i++) {
            DiscountRequest req = new DiscountRequest((long) (700 + i), new BigDecimal("10.00"));
            restTemplate.postForEntity("/api/v1/discounts", List.of(req), Void.class);
        }

        ResponseEntity<PageResponse<DiscountResponse>> response = restTemplate.exchange(
                "/api/v1/discounts/paged?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<PageResponse<DiscountResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
        assertThat(response.getBody().totalElements()).isEqualTo(5);
        assertThat(response.getBody().totalPages()).isEqualTo(3);
        assertThat(response.getBody().isFirst()).isTrue();
    }

    @Test
    void shouldSaveOutboxEventsAndProcessThemViaScheduler() {
        DiscountRequest request = new DiscountRequest(801L, new BigDecimal("45.00"));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/discounts", List.of(request), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        org.awaitility.Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Integer processedCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = '801' AND status = 'PROCESSED'",
                            Integer.class
                    );
                    assertThat(processedCount).isEqualTo(1);
                });
    }
}

