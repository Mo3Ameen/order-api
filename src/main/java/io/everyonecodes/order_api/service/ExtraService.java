package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.ExtraRequestDto;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExtraService {

    private final ExtraRepository repository;
    private final MenuItemRepository menuItemRepository;

    public ExtraService(ExtraRepository repository, MenuItemRepository menuItemRepository) {
        this.repository = repository;
        this.menuItemRepository = menuItemRepository;
    }

    public List<Extra> findByMenuItemsContainingAndIsActive(MenuItem menuItem) {
        return repository.findByMenuItemsContainingAndIsActive(menuItem, true);
    }

    public List<Extra> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Extra updateExtra(ExtraRequestDto dto, Long id) {
        Extra extra = findExtraByIdOrThrow(id);
        extra.setName(dto.getName());
        extra.setIsActive(dto.getIsActive());
        extra.setPrice(dto.getPrice());
        if (dto.getMenuItemIds() != null) {
            List<MenuItem> menuItems = findMenuItemsOrThrow(dto.getMenuItemIds());
            extra.setMenuItems(new HashSet<>(menuItems));
        }
        return repository.save(extra);
    }

    @Transactional
    public Extra createExtra(ExtraRequestDto dto) {
        Extra extra = new Extra();
        extra.setName(dto.getName());
        extra.setPrice(dto.getPrice());
        extra.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        if (dto.getMenuItemIds() != null) {
            List<MenuItem> menuItems = findMenuItemsOrThrow(dto.getMenuItemIds());
            extra.setMenuItems(new HashSet<>(menuItems));
        }
        return repository.save(extra);
    }

    public void softDeleteById(Long id) {
        Extra extra = findExtraByIdOrThrow(id);
        extra.setIsActive(false);
        repository.save(extra);
    }

    public Extra findExtraByIdOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Extra " + id + " not found"));
    }

    public List<Extra> findAllById(Set<Long> extraIds) {
        return repository.findAllById(extraIds);
    }

    @Transactional
    public void addMenuItem(MenuItem menuItem, Long extraId) {
        Extra extra = findExtraByIdOrThrow(extraId);
        extra.getMenuItems().add(menuItem);
        repository.save(extra);
    }

    @Transactional
    public void removeMenuItem(MenuItem menuItem, Long extraId) {
        Extra extra = findExtraByIdOrThrow(extraId);
        extra.getMenuItems().remove(menuItem);
        repository.save(extra);
    }

    private List<MenuItem> findMenuItemsOrThrow(Set<Long> ids) {
        List<MenuItem> menuItems = menuItemRepository.findAllById(ids);
        if (menuItems.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more menu items were not found");
        }
        return menuItems;
    }
}