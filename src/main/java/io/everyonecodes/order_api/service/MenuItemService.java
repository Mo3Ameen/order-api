package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuItemService {

    private final MenuItemRepository repository;

    public MenuItemService(MenuItemRepository repository) {
        this.repository = repository;
    }

    public List<MenuItem> findAll() {
        return repository.findAll();
    }

    public Optional<MenuItem> findById(Long id) {
        return repository.findById(id);
    }

    public MenuItem findByIdAndIsActiveOrThrow(Long id) {
        return repository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("MenuItem " + id + " not found"));
    }

    public List<MenuItem> findByCategoryAndIsActive(Category category) {
        return repository.findByCategoryAndIsActive(category, true);
    }

    public MenuItem save(MenuItem menuItem) {
        return repository.save(menuItem);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}