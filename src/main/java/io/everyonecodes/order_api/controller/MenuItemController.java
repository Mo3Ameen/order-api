package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.service.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menuItems")
public class MenuItemController {

    private final MenuItemService service;

    public MenuItemController(MenuItemService service) {
        this.service = service;
    }

    // ---------- Public requests for all Users ----------

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MenuItem> getActiveMenuItemsByCategory(@RequestParam Long categoryId) {
        return service.findByCategoryAndIsActive(categoryId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MenuItem getActiveMenuItemById(@PathVariable Long id) {
        return service.findByIdAndIsActiveOrThrow(id);
    }

    @GetMapping("/{id}/extras")
    @ResponseStatus(HttpStatus.OK)
    public List<Extra> getExtras(@PathVariable Long id) {
        return service.getExtras(id);
    }

    // ---------- Private requests for only Admins ----------

    @GetMapping("/all")
    public List<MenuItem> getAllMenuItems() {
        return service.findAll();
    }

    @GetMapping("/all/{id}")
    public MenuItem getMenuItem(@PathVariable Long id) {
        return service.findByIdOrThrow(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItem postMenuItem(@RequestBody MenuItemRequestDto menuItemRequestDto) {
        return service.postMenuItem(menuItemRequestDto);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MenuItem putMenuItem(@RequestBody MenuItemRequestDto menuItemRequestDto, @PathVariable Long id) {
        return service.putMenuItem(menuItemRequestDto, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(@PathVariable Long id) {
        service.softDeleteMenuItemById(id);
    }
}