package com.serjnn.DiscountService.controller;

import com.serjnn.DiscountService.dto.DiscountRequest;
import com.serjnn.DiscountService.dto.DiscountResponse;
import com.serjnn.DiscountService.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = DiscountController.class)
public class DiscountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DiscountService discountService;

    @Test
    public void testGetAllDiscounts() {
        DiscountResponse d1 = new DiscountResponse(1L, 100L, 10.0);
        DiscountResponse d2 = new DiscountResponse(2L, 101L, 15.0);

        when(discountService.findAll()).thenReturn(Flux.just(d1, d2));

        webTestClient.get()
                .uri("/api/v1/discounts")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DiscountResponse.class)
                .hasSize(2)
                .contains(d1, d2);
    }

    @Test
    public void testAddDiscounts() {
        DiscountRequest request = new DiscountRequest(100L, 10.0);

        when(discountService.addDiscounts(anyList())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(request))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    public void testGetDiscountByProductId_Found() {
        DiscountResponse d = new DiscountResponse(1L, 100L, 10.0);

        when(discountService.findByProductId(100L)).thenReturn(Mono.just(d));

        webTestClient.get()
                .uri("/api/v1/discounts/product/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DiscountResponse.class)
                .isEqualTo(d);
    }

    @Test
    public void testGetDiscountByProductId_NotFound() {
        when(discountService.findByProductId(100L)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/discounts/product/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }
}
