package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    // public/client methods
    public List<Category> findAllActive() {
        return repository.findAllByIsActive(true);
    }

    public Category getByIdAndIsActiveOrThrow(Long id) {
        return repository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }

    // private/admin methods
    public List<Category> findAll() {
        return repository.findAll();
    }

    public Category createCategory(Category category) {
        category.setId(null);
        category.setIsActive(category.getIsActive() != null ? category.getIsActive() : true);
        return repository.save(category);
    }

    public Category updateCategory(Category category, Long id) {
        Category existingCategory = findByIdOrThrow( id);
        existingCategory.setName(category.getName());
        existingCategory.setIsActive(category.getIsActive());
        return repository.save(existingCategory);
    }

    public void softDeleteCategoryById(Long id) {
        Category category = findByIdOrThrow(id);
        category.setIsActive(false);
        repository.save(category);
    }

    public Category findByIdOrThrow(Long categoryId) {
        return repository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
    }
}