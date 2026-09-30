package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // private/admin methods
    public MenuItem findByIdOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("MenuItem " + id + " not found"));
    }

    public void softDeleteMenuItemById( Long id) {
        MenuItem menuItem = findByIdOrThrow(id);
        menuItem.setIsActive(false);
        repository.save(menuItem);
    }

    @Transactional
    public MenuItem postMenuItem(MenuItemRequestDto menuItemRequestDto) {
        MenuItem menuItem = new MenuItem();
        menuItem.setPrice(menuItemRequestDto.getPrice());
        menuItem.setName(menuItemRequestDto.getName());
        menuItem.setDescription(menuItemRequestDto.getDescription());
        menuItem.setCategory(categoryService.findByIdOrThrow(menuItemRequestDto.getCategoryId()));
        menuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        menuItem.setIsActive(menuItemRequestDto.getIsActive() != null ? menuItemRequestDto.getIsActive() : true);
        MenuItem saved = repository.save(menuItem);
        syncExtras(saved, menuItemRequestDto.getExtraIds());
        return saved;
    }

    @Transactional
    public MenuItem putMenuItem(MenuItemRequestDto menuItemRequestDto, Long id) {
        MenuItem existingMenuItem = findByIdOrThrow(id);
        existingMenuItem.setCategory(categoryService.findByIdOrThrow(menuItemRequestDto.getCategoryId()));
        existingMenuItem.setPrice(menuItemRequestDto.getPrice());
        existingMenuItem.setName(menuItemRequestDto.getName());
        existingMenuItem.setDescription(menuItemRequestDto.getDescription());
        existingMenuItem.setIsActive(menuItemRequestDto.getIsActive());
        existingMenuItem.setImageUrl(menuItemRequestDto.getImageUrl());
        syncExtras(existingMenuItem, menuItemRequestDto.getExtraIds());
        return repository.save(existingMenuItem);
    }

    public List<MenuItem> findAll() {
        return repository.findAll();
    }

    private void syncExtras(MenuItem menuItem, Set<Long> extraIds) {
        if (extraIds == null) {
            return;
        }

        List<Extra> wantedExtras = extraService.findAllById(extraIds);
        if (wantedExtras.size() != extraIds.size()) {
            throw new ResourceNotFoundException("One or more extras were not found");
        }

        Set<Extra> currentExtras = new HashSet<>(menuItem.getExtras());
        for (Extra extra : currentExtras) {
            if (!wantedExtras.contains(extra)) {
                extraService.removeMenuItem(menuItem, extra.getId());
            }
        }

        for (Extra extra : wantedExtras) {
            if (!currentExtras.contains(extra)) {
                extraService.addMenuItem(menuItem, extra.getId());
            }
        }

        menuItem.getExtras().clear();
        menuItem.getExtras().addAll(wantedExtras);
    }
}