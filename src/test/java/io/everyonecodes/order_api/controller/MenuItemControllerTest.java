package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.repository.CategoryRepository;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
@Transactional
@AutoConfigureMockMvc
class MenuItemControllerTest {

    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected MenuItemRepository menuItemRepository;
    @Autowired
    protected ExtraRepository extraRepository;

    protected Category activeCategory;
    protected Category inactiveCategory;
    protected MenuItem activeMenuItem;
    protected MenuItem inactiveMenuItem;
    protected Extra activeExtra;
    protected Extra inactiveExtra;

    @BeforeEach
    void setUpFixtures() {
        activeCategory = new Category();
        activeCategory.setName("Burgers");
        activeCategory.setIsActive(true);
        activeCategory = categoryRepository.save(activeCategory);

        inactiveCategory = new Category();
        inactiveCategory.setName("Discontinued Category");
        inactiveCategory.setIsActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);

        activeMenuItem = new MenuItem();
        activeMenuItem.setName("Cheeseburger");
        activeMenuItem.setDescription("Beef burger");
        activeMenuItem.setPrice(BigDecimal.valueOf(10));
        activeMenuItem.setIsActive(true);
        activeMenuItem.setCategory(activeCategory);
        activeMenuItem = menuItemRepository.save(activeMenuItem);

        inactiveMenuItem = new MenuItem();
        inactiveMenuItem.setName("Discontinued Burger");
        inactiveMenuItem.setDescription("No longer sold");
        inactiveMenuItem.setPrice(BigDecimal.valueOf(8));
        inactiveMenuItem.setIsActive(false);
        inactiveMenuItem.setCategory(activeCategory);
        inactiveMenuItem = menuItemRepository.save(inactiveMenuItem);

        activeExtra = new Extra();
        activeExtra.setName("Extra Cheese");
        activeExtra.setPrice(BigDecimal.valueOf(1.5));
        activeExtra.setIsActive(true);
        activeExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(activeExtra);
        activeExtra = extraRepository.save(activeExtra);

        inactiveExtra = new Extra();
        inactiveExtra.setName("Mushrooms");
        inactiveExtra.setPrice(BigDecimal.valueOf(1));
        inactiveExtra.setIsActive(false);
        inactiveExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(inactiveExtra);
        inactiveExtra = extraRepository.save(inactiveExtra);
    }

    // ---------- GET /api/menuItems?categoryId= ----------

    @Test
    void getActiveMenuItemsByCategory_returnsOnlyActiveMenuItems() {
        var result = restTestClient.get()
                .uri("/api/menuItems?categoryId={categoryId}", activeCategory.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItem[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        var names = Stream.of(result).map(MenuItem::getName).toList();
        assertTrue(names.contains(activeMenuItem.getName()));
        assertFalse(names.contains(inactiveMenuItem.getName()));
    }

    @Test
    void getActiveMenuItemsByCategory_returns404_whenCategoryIsInactive() {
        var result = restTestClient.get()
                .uri("/api/menuItems?categoryId={categoryId}", inactiveCategory.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("Category " + inactiveCategory.getId() + " not found", result);
    }

    @Test
    void getActiveMenuItemsByCategory_returns404_whenCategoryDoesNotExist() {
        Long nonExistentId = 999999L;

        var result = restTestClient.get()
                .uri("/api/menuItems?categoryId={categoryId}", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("Category " + nonExistentId + " not found", result);
    }

    // ---------- GET /api/menuItems/{id} ----------

    @Test
    void getMenuItemById_returnsActiveMenuItem_whenActive() {
        var result = restTestClient.get()
                .uri("/api/menuItems/{id}", activeMenuItem.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItem.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(activeMenuItem.getId(), result.getId());
        assertEquals(activeMenuItem.getName(), result.getName());
    }

    @Test
    void getMenuItemById_returns404_whenActiveMenuItemIsInactive() {
        var result = restTestClient.get()
                .uri("/api/menuItems/{id}", inactiveMenuItem.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("MenuItem " + inactiveMenuItem.getId() + " not found", result);
    }

    @Test
    void getMenuItemById_returns404_whenActiveMenuItemDoesNotExist() {
        Long nonExistentId = 999999L;

        var result = restTestClient.get()
                .uri("/api/menuItems/{id}", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("MenuItem " + nonExistentId + " not found", result);
    }

    // ---------- GET /api/menuItems/{id}/extras ----------

    @Test
    void getExtras_returnsOnlyActiveExtras() {
        var result = restTestClient.get()
                .uri("/api/menuItems/{id}/extras", activeMenuItem.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Extra[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        var names = Stream.of(result).map(Extra::getName).toList();
        assertTrue(names.contains(activeExtra.getName()));
        assertFalse(names.contains(inactiveExtra.getName()));
    }

    @Test
    void getExtras_returns404_whenMenuItemIsInactive() {
        var result = restTestClient.get()
                .uri("/api/menuItems/{id}/extras", inactiveMenuItem.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("MenuItem " + inactiveMenuItem.getId() + " not found", result);
    }

    @Test
    void getExtras_returns404_whenMenuItemDoesNotExist() {
        Long nonExistentId = 999999L;

        var result = restTestClient.get()
                .uri("/api/menuItems/{id}/extras", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("MenuItem " + nonExistentId + " not found", result);
    }

    @Test
    void adminEndpoints_returnAllMenuItemsAndItemsById() {
        var allItems = restTestClient.get()
                .uri("/api/menuItems/all")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItem[].class)
                .returnResult().getResponseBody();
        var item = restTestClient.get()
                .uri("/api/menuItems/all/{id}", inactiveMenuItem.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItem.class)
                .returnResult().getResponseBody();

        assertNotNull(allItems);
        assertTrue(Stream.of(allItems).anyMatch(menuItem -> menuItem.getId().equals(activeMenuItem.getId())));
        assertTrue(Stream.of(allItems).anyMatch(menuItem -> menuItem.getId().equals(inactiveMenuItem.getId())));
        assertNotNull(item);
        assertEquals(inactiveMenuItem.getId(), item.getId());
    }

    @Test
    void postMenuItem_createsAnActiveMenuItemByDefault() {
        var request = new MenuItemRequestDto("Veggie Burger", "Plant based", BigDecimal.valueOf(11), "veggie.jpg", null, activeCategory.getId());

        var result = restTestClient.post()
                .uri("/api/menuItems")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MenuItem.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Veggie Burger", result.getName());
        assertTrue(result.getIsActive());
        assertEquals(activeCategory.getId(), result.getCategory().getId());
    }

    @Test
    void putMenuItem_updatesEveryField() {
        var request = new MenuItemRequestDto("Updated Burger", "New description", BigDecimal.valueOf(12), "updated.jpg", false, activeCategory.getId());

        var result = restTestClient.put()
                .uri("/api/menuItems/{id}", activeMenuItem.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MenuItem.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals("Updated Burger", result.getName());
        assertEquals("New description", result.getDescription());
        assertEquals(BigDecimal.valueOf(12), result.getPrice());
        assertEquals("updated.jpg", result.getImageUrl());
        assertFalse(result.getIsActive());
    }

    @Test
    void deleteMenuItem_softDeletesTheMenuItem() {
        restTestClient.delete()
                .uri("/api/menuItems/{id}", activeMenuItem.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNoContent();

        assertFalse(menuItemRepository.findById(activeMenuItem.getId()).orElseThrow().getIsActive());
    }
}
