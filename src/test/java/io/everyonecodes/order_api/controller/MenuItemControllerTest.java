package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
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
    void getMenuItemById_returnsMenuItem_whenActive() {
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
    void getMenuItemById_returns404_whenMenuItemIsInactive() {
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
    void getMenuItemById_returns404_whenMenuItemDoesNotExist() {
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
}