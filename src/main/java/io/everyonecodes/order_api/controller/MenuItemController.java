package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.service.CategoryService;
import io.everyonecodes.order_api.service.ExtraService;
import io.everyonecodes.order_api.service.MenuItemService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menuItems")
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final CategoryService categoryService;
    private final ExtraService extraService;

    public MenuItemController(MenuItemService menuItemService, CategoryService categoryService, ExtraService extraService) {
        this.menuItemService = menuItemService;
        this.categoryService = categoryService;
        this.extraService = extraService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MenuItem> getMenuItems(@RequestParam Long categoryId) {
        Category category = categoryService.getByIdOrThrow(categoryId);
        return menuItemService.findByCategoryAndIsActive(category);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MenuItem getMenuItemById(@PathVariable Long id) {
        return menuItemService.findByIdAndIsActiveOrThrow(id);
    }

    @GetMapping("/{id}/extras")
    @ResponseStatus(HttpStatus.OK)
    public List<Extra> getExtras(@PathVariable Long id) {
        MenuItem item = menuItemService.findByIdAndIsActiveOrThrow(id);
        return extraService.findByMenuItemsContainingAndIsActive(item);
    }

    @PostMapping
    public ResponseEntity<MenuItem> postMenuItem(@RequestBody MenuItemRequestDto menuItemRequestDto) {
        MenuItem menuItem = new MenuItem();
        menuItem.setPrice(menuItemRequestDto.getPrice());
        menuItem.setName(menuItemRequestDto.getName());
        menuItem.setIsActive(menuItemRequestDto.getIsActive());
        menuItem.setDescription(menuItemRequestDto.getDescription());
        menuItem.setCategory(categoryService.getByIdOrThrow(menuItemRequestDto.getCategoryId()));
        menuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        menuItem.setIsActive(menuItemRequestDto.getIsActive() != null ? menuItemRequestDto.getIsActive() : true);
        MenuItem saved = menuItemService.save(menuItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItem> putMenuItem(@RequestBody MenuItemRequestDto menuItemRequestDto, @PathVariable Long id) {
        MenuItem existingMenuItem = getMenuItem(id);
        existingMenuItem.setCategory(categoryService.getByIdOrThrow(menuItemRequestDto.getCategoryId()));
        existingMenuItem.setPrice(menuItemRequestDto.getPrice());
        existingMenuItem.setName(menuItemRequestDto.getName());
        existingMenuItem.setDescription(menuItemRequestDto.getDescription());
        existingMenuItem.setIsActive(menuItemRequestDto.getIsActive());
        existingMenuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        MenuItem saved = menuItemService.save(existingMenuItem);
        return ResponseEntity.status(HttpStatus.OK).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItemById(@PathVariable Long id) {
        MenuItem menuItem = getMenuItem(id);
        menuItem.setIsActive(false);
        menuItemService.save(menuItem);
        return ResponseEntity.noContent().build();
    }

    private @NonNull MenuItem getMenuItem(Long id) {
        return menuItemService.findById(id).orElseThrow(() -> new ResourceNotFoundException("MenuItem " + id + " not found"));
    }
}