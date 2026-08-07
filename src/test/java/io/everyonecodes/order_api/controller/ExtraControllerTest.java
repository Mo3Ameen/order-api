package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.repository.ExtraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
@Transactional
class ExtraControllerTest {

    @Autowired
    private RestTestClient restTestClient;
    @Autowired
    private ExtraRepository extraRepository;

    private Extra activeExtra;
    private Extra inactiveExtra;

    @BeforeEach
    void setUpFixtures() {
        activeExtra = extraRepository.save(new Extra(null, "Cheese", BigDecimal.valueOf(1.5), true, null));
        inactiveExtra = extraRepository.save(new Extra(null, "Mushrooms", BigDecimal.ONE, false, null));
    }

    @Test
    void getExtras_returnsAllExtras() {
        var result = restTestClient.get()
                .uri("/api/extras")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra[].class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertTrue(java.util.Arrays.stream(result).anyMatch(extra -> extra.getId().equals(activeExtra.getId())));
        assertTrue(java.util.Arrays.stream(result).anyMatch(extra -> extra.getId().equals(inactiveExtra.getId())));
    }

    @Test
    void getExtraById_returnsExtraOrNotFound() {
        var result = restTestClient.get()
                .uri("/api/extras/{id}", activeExtra.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals(activeExtra.getId(), result.getId());

        restTestClient.get()
                .uri("/api/extras/{id}", 999999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("Extra 999999 not found");
    }

    @Test
    void postExtra_createsAnActiveExtraByDefault() {
        var request = new Extra(null, "Bacon", BigDecimal.valueOf(2), null, null);

        var result = restTestClient.post()
                .uri("/api/extras")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Extra.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertNotNull(result.getId());
        assertTrue(result.getIsActive());
    }

    @Test
    void putExtra_updatesEveryMutableField() {
        var request = new Extra(null, "Double Cheese", BigDecimal.valueOf(2.5), false, null);

        var result = restTestClient.put()
                .uri("/api/extras/{id}", activeExtra.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals("Double Cheese", result.getName());
        assertEquals(BigDecimal.valueOf(2.5), result.getPrice());
        assertFalse(result.getIsActive());
    }

    @Test
    void deleteExtra_softDeletesTheExtra() {
        restTestClient.delete()
                .uri("/api/extras/{id}", activeExtra.getId())
                .exchange()
                .expectStatus().isNoContent();

        assertFalse(extraRepository.findById(activeExtra.getId()).orElseThrow().getIsActive());
        assertFalse(inactiveExtra.getIsActive());
    }
}
