package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    void getCategoryById_returnsCategory_whenActiveCategoryExists() {
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
    void getCategoryById_returns404_whenCategoryIsInactive() {
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
    void getCategoryById_returns404_whenCategoryDoesNotExist() {
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
}