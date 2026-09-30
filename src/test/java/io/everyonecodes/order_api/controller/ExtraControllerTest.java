package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.ExtraRequestDto;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
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
import java.util.HashSet;
import java.util.Set;

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
    @Autowired
    private MenuItemRepository menuItemRepository;

    private Extra activeExtra;
    private Extra inactiveExtra;
    private MenuItem burger;
    private MenuItem wrap;

    @BeforeEach
    void setUpFixtures() {
        activeExtra = extraRepository.save(new Extra(null, "Cheese", BigDecimal.valueOf(1.5), true, null));
        inactiveExtra = extraRepository.save(new Extra(null, "Mushrooms", BigDecimal.ONE, false, null));

        burger = saveMenuItem("Burger");
        wrap = saveMenuItem("Wrap");
        activeExtra.setMenuItems(new HashSet<>(Set.of(burger)));
        activeExtra = extraRepository.save(activeExtra);
    }

    private MenuItem saveMenuItem(String name) {
        var menuItem = new MenuItem();
        menuItem.setName(name);
        menuItem.setDescription("Fixture item");
        menuItem.setPrice(BigDecimal.TEN);
        menuItem.setIsActive(true);
        return menuItemRepository.save(menuItem);
    }

    private Extra[] getPublicExtrasOf(MenuItem menuItem) {
        return restTestClient.get()
                .uri("/api/menuItems/{id}/extras", menuItem.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra[].class)
                .returnResult().getResponseBody();
    }

    @Test
    void getExtras_returnsAllExtras() {
        var result = restTestClient.get()
                .uri("/api/extras")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
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
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals(activeExtra.getId(), result.getId());

        restTestClient.get()
                .uri("/api/extras/{id}", 999999L)
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .isEqualTo("Extra 999999 not found");
    }

    @Test
    void postExtra_createsAnActiveExtraByDefault() {
        var request = new ExtraRequestDto("Bacon", BigDecimal.valueOf(2), null, null);

        var result = restTestClient.post()
                .uri("/api/extras")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
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
        var request = new ExtraRequestDto("Double Cheese", BigDecimal.valueOf(2.5), false, null);

        var result = restTestClient.put()
                .uri("/api/extras/{id}", activeExtra.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
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
    void postExtra_linksTheRequestedMenuItems() {
        var request = new ExtraRequestDto("Bacon", BigDecimal.valueOf(2), true, Set.of(wrap.getId()));

        var result = restTestClient.post()
                .uri("/api/extras")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Extra.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        var extrasOfWrap = getPublicExtrasOf(wrap);
        assertEquals(1, extrasOfWrap.length);
        assertEquals(result.getId(), extrasOfWrap[0].getId());
    }

    @Test
    void postExtra_returns404_whenAMenuItemDoesNotExist() {
        var request = new ExtraRequestDto("Ghost Extra", BigDecimal.valueOf(2), true, Set.of(999999L));

        var result = restTestClient.post()
                .uri("/api/extras")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertEquals("One or more menu items were not found", result);
    }

    @Test
    void putExtra_replacesTheLinkedMenuItems() {
        var request = new ExtraRequestDto("Cheese", BigDecimal.valueOf(1.5), true, Set.of(wrap.getId()));

        restTestClient.put()
                .uri("/api/extras/{id}", activeExtra.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        assertEquals(0, getPublicExtrasOf(burger).length);
        var extrasOfWrap = getPublicExtrasOf(wrap);
        assertEquals(1, extrasOfWrap.length);
        assertEquals(activeExtra.getId(), extrasOfWrap[0].getId());
    }

    @Test
    void putExtra_withEmptyMenuItemIds_removesAllLinks() {
        var request = new ExtraRequestDto("Cheese", BigDecimal.valueOf(1.5), true, Set.of());

        restTestClient.put()
                .uri("/api/extras/{id}", activeExtra.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        assertEquals(0, getPublicExtrasOf(burger).length);
        assertEquals(0, getPublicExtrasOf(wrap).length);
    }

    @Test
    void putExtra_withoutMenuItemIds_keepsTheLinks() {
        var request = new ExtraRequestDto("Cheese", BigDecimal.valueOf(1.5), true, null);

        restTestClient.put()
                .uri("/api/extras/{id}", activeExtra.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        var extrasOfBurger = getPublicExtrasOf(burger);
        assertEquals(1, extrasOfBurger.length);
        assertEquals(activeExtra.getId(), extrasOfBurger[0].getId());
    }

    @Test
    void deleteExtra_softDeletesTheExtra() {
        restTestClient.delete()
                .uri("/api/extras/{id}", activeExtra.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNoContent();

        assertFalse(extraRepository.findById(activeExtra.getId()).orElseThrow().getIsActive());
        assertFalse(inactiveExtra.getIsActive());
    }
}
