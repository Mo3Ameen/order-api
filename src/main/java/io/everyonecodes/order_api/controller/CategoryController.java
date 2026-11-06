package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.CategoryItemDto;
import io.everyonecodes.order_api.dto.CategoryReactivationRequestDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    // ---------- Public requests for all Users ----------

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Category> getActiveCategories() {
        return service.findAllActive();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Category getActiveCategoryById(@PathVariable Long id) {
        return service.getByIdAndIsActiveOrThrow(id);
    }

    // ---------- Private requests for only Admins ----------

    @GetMapping("/all")
    public List<Category> getAllCategories() {
        return service.findAll();
    }

    @GetMapping("/all/{id}")
    public Category getCategory(@PathVariable Long id) {
        return service.findByIdOrThrow(id);
    }

    @GetMapping("/all/{id}/items")
    public List<CategoryItemDto> getItemsByCategory(@PathVariable Long id) {
        return service.findItemsOfCategory(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category postCategory(@RequestBody Category category) {
        return service.createCategory(category);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Category putCategory(@RequestBody Category category, @PathVariable Long id) {
        return service.updateCategory(category, id);
    }

    @PutMapping(value = "/{id}/reactivate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Category reactivateCategory(@PathVariable Long id, @RequestBody CategoryReactivationRequestDto request) {
        return service.reactivateCategory(id, request.getMenuItemIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        service.softDeleteCategoryById(id);
    }
}