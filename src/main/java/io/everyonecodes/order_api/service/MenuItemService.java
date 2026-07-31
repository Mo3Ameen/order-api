package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenuItemService {

    private final MenuItemRepository repository;
    private final CategoryService categoryService;
    private final ExtraService extraService;

    public MenuItemService(MenuItemRepository repository, CategoryService categoryService, ExtraService extraService) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.extraService = extraService;
    }

    // public/client methods
    public MenuItem findByIdAndIsActiveOrThrow(Long id) {
        return repository.findByIdAndIsActiveTrue(id).orElseThrow(() -> new ResourceNotFoundException("MenuItem " + id + " not found"));
    }

    public List<MenuItem> findByCategoryAndIsActive(Long categoryId) {
        Category category = categoryService.getByIdAndIsActiveOrThrow(categoryId);
        return repository.findByCategoryAndIsActive(category, true);
    }

    public List<Extra> getExtras(Long id) {
        MenuItem item = findByIdAndIsActiveOrThrow(id);
        return extraService.findByMenuItemsContainingAndIsActive(item);
    }

    // private/admin methods (for later)
    public MenuItem findByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException("MenuItem " + id + " not found"));
    }

    public void softDeleteMenuItemById( Long id) {
        MenuItem menuItem = findByIdOrThrow(id);
        menuItem.setIsActive(false);
        save(menuItem);
    }

    public MenuItem postMenuItem(MenuItemRequestDto menuItemRequestDto) {
        MenuItem menuItem = new MenuItem();
        menuItem.setPrice(menuItemRequestDto.getPrice());
        menuItem.setName(menuItemRequestDto.getName());
        menuItem.setDescription(menuItemRequestDto.getDescription());
        menuItem.setCategory(categoryService.getByIdOrThrow(menuItemRequestDto.getCategoryId()));
        menuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        menuItem.setIsActive(menuItemRequestDto.getIsActive() != null ? menuItemRequestDto.getIsActive() : true);
        return save(menuItem);
    }

    public MenuItem putMenuItem(MenuItemRequestDto menuItemRequestDto, Long id) {
        MenuItem existingMenuItem = findByIdOrThrow(id);
        existingMenuItem.setCategory(categoryService.getByIdOrThrow(menuItemRequestDto.getCategoryId()));
        existingMenuItem.setPrice(menuItemRequestDto.getPrice());
        existingMenuItem.setName(menuItemRequestDto.getName());
        existingMenuItem.setDescription(menuItemRequestDto.getDescription());
        existingMenuItem.setIsActive(menuItemRequestDto.getIsActive());
        existingMenuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        return save(existingMenuItem);
    }

    public List<MenuItem> findAll() {
        return repository.findAll();
    }

    public Optional<MenuItem> findById(Long id) {
        return repository.findById(id);
    }

    public MenuItem save(MenuItem menuItem) {
        return repository.save(menuItem);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}