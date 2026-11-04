package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.CategoryItemDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final MenuItemRepository menuItemRepository;

    public CategoryService(CategoryRepository repository, MenuItemRepository menuItemRepository) {
        this.repository = repository;
        this.menuItemRepository = menuItemRepository;
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
        category.setIsActive(category.getIsActive() == null || category.getIsActive());
        return repository.save(category);
    }

    @Transactional
    public Category updateCategory(Category category, Long id) {
        Category existingCategory = findByIdOrThrow(id);
        boolean isBeingTurnedOf = Boolean.TRUE.equals(existingCategory.getIsActive()) && Boolean.FALSE.equals(category.getIsActive());
        existingCategory.setName(category.getName());
        existingCategory.setIsActive(category.getIsActive());
        if (isBeingTurnedOf) {
            switchOffActiveItems(existingCategory);
        }
        return repository.save(existingCategory);
    }

    @Transactional
    public void softDeleteCategoryById(Long id) {
        Category category = findByIdOrThrow(id);
        category.setIsActive(false);
        repository.save(category);
        switchOffActiveItems(category);
    }

    public Category findByIdOrThrow(Long categoryId) {
        return repository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category " + categoryId + " not found"));
    }

    public List<CategoryItemDto> findItemsOfCategory(Long categoryId) {
        Category category = findByIdOrThrow(categoryId);
        return menuItemRepository.findByCategory(category).stream()
                .map(item -> new CategoryItemDto(
                        item.getId(),
                        item.getName(),
                        item.getPrice(),
                        item.getIsActive(),
                        item.getDeactivatedWithCategory()))
                .toList();
    }

    private void switchOffActiveItems(Category category) {
        List<MenuItem> activeItems = menuItemRepository.findByCategoryAndIsActive(category, true);
        activeItems.forEach(item -> {
            item.setIsActive(false);
            item.setDeactivatedWithCategory(true);
        });
        menuItemRepository.saveAll(activeItems);
    }
}