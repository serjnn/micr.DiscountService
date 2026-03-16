package com.serjnn.DiscountService;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
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
        jdbcTemplate.execute("TRUNCATE TABLE discount_entity RESTART IDENTITY CASCADE");
        kafkaMessages.clear();
    }

    @KafkaListener(topics = "discountChangesTopic", groupId = "test-group")
    public void listen(DiscountChangesDto message) {
        kafkaMessages.add(message);
    }

    @Test
    void shouldCreateAndThenUpdateDiscount() throws InterruptedException {
        // 1. Add new discount
        DiscountRequest request = new DiscountRequest(101L, 15.5);
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/discounts", List.of(request), Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Verify it's in DB
        ResponseEntity<DiscountResponse> getResponse = restTemplate.getForEntity("/api/v1/discounts/101", DiscountResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().productId()).isEqualTo(101L);
        assertThat(getResponse.getBody().discount()).isEqualTo(15.5);

        // 3. Verify Kafka message for NEW discount
        DiscountChangesDto firstMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(firstMessage).isNotNull();
        assertThat(firstMessage.productId()).isEqualTo(101L);
        assertThat(firstMessage.newDiscount()).isEqualTo(15.5);
        assertThat(firstMessage.prevDiscount()).isEqualTo(0.0);

        // 4. Update existing discount
        DiscountRequest updateRequest = new DiscountRequest(101L, 20.0);
        restTemplate.postForEntity("/api/v1/discounts", List.of(updateRequest), Void.class);

        // 5. Verify updated in DB
        ResponseEntity<DiscountResponse> updatedGetResponse = restTemplate.getForEntity("/api/v1/discounts/101", DiscountResponse.class);
        assertThat(updatedGetResponse.getBody().discount()).isEqualTo(20.0);

        // 6. Verify Kafka message for UPDATED discount
        DiscountChangesDto secondMessage = kafkaMessages.poll(10, TimeUnit.SECONDS);
        assertThat(secondMessage).isNotNull();
        assertThat(secondMessage.productId()).isEqualTo(101L);
        assertThat(secondMessage.newDiscount()).isEqualTo(20.0);
        assertThat(secondMessage.prevDiscount()).isEqualTo(15.5);
    }

    @Test
    void shouldGetAllDiscounts() {
        DiscountRequest req1 = new DiscountRequest(201L, 10.0);
        DiscountRequest req2 = new DiscountRequest(202L, 20.0);
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
}
