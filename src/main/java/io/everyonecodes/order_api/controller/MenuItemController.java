package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.service.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menuItems")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MenuItem> getActiveMenuItemsByCategory(@RequestParam Long categoryId) {
        return menuItemService.findByCategoryAndIsActive(categoryId);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MenuItem getMenuItemById(@PathVariable Long id) {
        return menuItemService.findByIdAndIsActiveOrThrow(id);
    }

    @GetMapping("/{id}/extras")
    @ResponseStatus(HttpStatus.OK)
    public List<Extra> getExtras(@PathVariable Long id) {
        return menuItemService.getExtras(id);
    }
}