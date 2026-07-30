package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> findAll() {
        return repository.findAll();
    }

    public List<Category> findAllActive() {
        return repository.findAllByIsActive(true);
    }

    public Optional<Category> findById(Long id) {
        return repository.findById(id);
    }

    public Category save(Category category) {
        return repository.save(category);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public Category getByIdOrThrow(Long id) {
        return repository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }
}