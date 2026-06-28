package com.serjnn.DiscountService;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.repository.DiscountRepository;
import com.serjnn.DiscountService.service.DiscountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Disabled by default because running Testcontainers requires a local Docker daemon (e.g. Docker Desktop) to be active.")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.sql.init.mode=always"
})
public class DiscountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private DiscountService discountService;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    public void setUp() {
        discountRepository.deleteAll().block();
    }

    @Test
    public void testCreateAndRetrieveDiscount() {
        DiscountRequest request = new DiscountRequest(100L, 15.0);

        discountService.addDiscounts(List.of(request)).block();

        List<DiscountResponse> discounts = discountService.findAll().collectList().block();
        assertThat(discounts).isNotNull();
        assertThat(discounts).hasSize(1);
        assertThat(discounts.get(0).productId()).isEqualTo(100L);
        assertThat(discounts.get(0).discount()).isEqualTo(15.0);

        webTestClient.get()
                .uri("/api/v1/discounts/product/100")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DiscountResponse.class)
                .value(response -> {
                    assertThat(response.productId()).isEqualTo(100L);
                    assertThat(response.discount()).isEqualTo(15.0);
                });
    }
}
