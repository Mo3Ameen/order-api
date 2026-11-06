package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.CategoryItemDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.CategoryRepository;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
@Transactional
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected MenuItemRepository menuItemRepository;

    protected Category activeCategory;
    protected Category inactiveCategory;

    @BeforeEach
    void setUpFixtures() {
        activeCategory = new Category();
        activeCategory.setName("Burgers");
        activeCategory.setIsActive(true);
        activeCategory = categoryRepository.save(activeCategory);

        inactiveCategory = new Category();
        inactiveCategory.setName("Discontinued");
        inactiveCategory.setIsActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);
    }

    // ---------- GET /api/categories ----------

    @Test
    void getCategories_returnsOnlyActiveCategories() {
        var result = restTestClient.get()
                .uri("/api/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category[].class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        var names = Stream.of(result).map(Category::getName).toList();
        assertTrue(names.contains(activeCategory.getName()));
        assertFalse(names.contains(inactiveCategory.getName()));
    }

    // ---------- GET /api/categories/{id} ----------

    @Test
    void getCategoryById_returnsCategory_whenActiveActiveCategoryExists() {
        var result = restTestClient.get()
                .uri("/api/categories/{id}", activeCategory.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(result);
        assertEquals(activeCategory.getId(), result.getId());
        assertEquals(activeCategory.getName(), result.getName());
    }

    @Test
    void getCategoryById_returns404_whenActiveCategoryIsInactive() {
        var result = restTestClient.get()
                .uri("/api/categories/{id}", inactiveCategory.getId())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("Category " + inactiveCategory.getId() + " not found", result);
    }

    @Test
    void getCategoryById_returns404_whenActiveCategoryDoesNotExist() {
        Long nonExistentId = 999999L;

        var result = restTestClient.get()
                .uri("/api/categories/{id}", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertEquals("Category " + nonExistentId + " not found", result);
    }

    @Test
    void adminEndpoints_returnAllCategoriesAndCategoriesById() {
        var allCategories = restTestClient.get()
                .uri("/api/categories/all")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category[].class)
                .returnResult().getResponseBody();

        var category = restTestClient.get()
                .uri("/api/categories/all/{id}", inactiveCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category.class)
                .returnResult().getResponseBody();

        assertNotNull(allCategories);
        assertTrue(Stream.of(allCategories).anyMatch(candidate -> candidate.getId().equals(activeCategory.getId())));
        assertTrue(Stream.of(allCategories).anyMatch(candidate -> candidate.getId().equals(inactiveCategory.getId())));
        assertNotNull(category);
        assertEquals(inactiveCategory.getId(), category.getId());
    }

    @Test
    void postCategory_createsAnActiveCategoryByDefault() {
        var request = new Category();
        request.setName("Salads");

        var result = restTestClient.post()
                .uri("/api/categories")
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Category.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Salads", result.getName());
        assertTrue(result.getIsActive());
    }

    @Test
    void putCategory_updatesTheCategory() {
        var request = new Category();
        request.setName("Updated Burgers");
        request.setIsActive(false);

        var result = restTestClient.put()
                .uri("/api/categories/{id}", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category.class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals("Updated Burgers", result.getName());
        assertFalse(result.getIsActive());
    }

    @Test
    void deleteCategory_softDeletesTheCategory() {
        restTestClient.delete()
                .uri("/api/categories/{id}", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNoContent();

        assertFalse(categoryRepository.findById(activeCategory.getId()).orElseThrow().getIsActive());
    }

    // ---------- turning a category off, and GET /api/categories/all/{id}/items ----------

    private MenuItem saveItem(Category category, String name, boolean isActive) {
        var item = new MenuItem();
        item.setName(name);
        item.setDescription("test");
        item.setPrice(new BigDecimal("9.50"));
        item.setIsActive(isActive);
        item.setCategory(category);
        return menuItemRepository.save(item);
    }

    @Test
    void deleteCategory_switchesOffItsActiveItemsAndRemembersThem() {
        saveItem(activeCategory, "Cheeseburger", true);
        saveItem(activeCategory, "Old Burger", false);

        restTestClient.delete()
                .uri("/api/categories/{id}", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNoContent();

        var result = restTestClient.get()
                .uri("/api/categories/all/{id}/items", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryItemDto[].class)
                .returnResult().getResponseBody();

        assertNotNull(result);
        var byName = Stream.of(result).collect(Collectors.toMap(CategoryItemDto::getName, dto -> dto));
        assertFalse(byName.get("Cheeseburger").getIsActive());
        assertTrue(byName.get("Cheeseburger").getWasActiveBeforeTurnOff());
        assertFalse(byName.get("Old Burger").getIsActive());
        assertFalse(byName.get("Old Burger").getWasActiveBeforeTurnOff());
    }

    @Test
    void putCategory_switchesOffItsActiveItems_whenTurnedOff() {
        var item = saveItem(activeCategory, "Cheeseburger", true);
        var request = new Category();
        request.setName("Burgers");
        request.setIsActive(false);

        restTestClient.put()
                .uri("/api/categories/{id}", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        var saved = menuItemRepository.findById(item.getId()).orElseThrow();
        assertFalse(saved.getIsActive());
        assertTrue(saved.getDeactivatedWithCategory());
    }

    @Test
    void putCategory_leavesItsItemsAlone_whenOnlyRenamed() {
        var item = saveItem(activeCategory, "Cheeseburger", true);
        var request = new Category();
        request.setName("Renamed Burgers");
        request.setIsActive(true);

        restTestClient.put()
                .uri("/api/categories/{id}", activeCategory.getId())
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk();

        var saved = menuItemRepository.findById(item.getId()).orElseThrow();
        assertTrue(saved.getIsActive());
        assertFalse(saved.getDeactivatedWithCategory());
    }

    @Test
    void getItemsOfCategory_returns404_whenTheCategoryDoesNotExist() {
        var result = restTestClient.get()
                .uri("/api/categories/all/{id}/items", 999999L)
                .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertEquals("Category 999999 not found", result);
    }

    @Test
    void getItemsOfCategory_returns401_whenNotAuthenticated() {
        restTestClient.get()
                .uri("/api/categories/all/{id}/items", activeCategory.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
