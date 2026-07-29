package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.service.CategoryService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Category> getCategories(@RequestParam(required = false) Boolean isActive) {
        if (isActive != null && isActive) {
            return service.findAllActive();
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Category getCategoryById(@PathVariable Long id) {
        return service.getByIdOrThrow(id);
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        category.setId(null);
        Category saved = service.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@RequestBody Category category, @PathVariable Long id) {
        Category existingCategory = service.findById(id).orElseThrow(() -> new ResourceNotFoundException(("Category " + id + " not found")));
        existingCategory.setName(category.getName());
        existingCategory.setIsActive(category.getIsActive());
        Category saved = service.save(existingCategory);
        return ResponseEntity.status(HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoryById(@PathVariable Long id) {
        service.getByIdOrThrow(id);
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}