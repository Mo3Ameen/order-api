package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

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
                .exchange()
                .expectStatus().isOk()
                .expectBody(Category[].class)
                .returnResult().getResponseBody();

        var category = restTestClient.get()
                .uri("/api/categories/all/{id}", inactiveCategory.getId())
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
                .exchange()
                .expectStatus().isNoContent();

        assertFalse(categoryRepository.findById(activeCategory.getId()).orElseThrow().getIsActive());
    }
}
