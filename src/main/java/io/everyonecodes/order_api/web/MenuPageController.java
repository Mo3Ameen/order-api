package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.service.CategoryService;
import io.everyonecodes.order_api.service.MenuItemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MenuPageController {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;

    public MenuPageController(CategoryService categoryService, MenuItemService menuItemService) {
        this.categoryService = categoryService;
        this.menuItemService = menuItemService;
    }

    @GetMapping("/menu")
    public String showCategories(Model model) {
        model.addAttribute("categories", categoryService.findAllActive());
        return "menu/categories";
    }

    @GetMapping("/menu/categories/{categoryId}")
    public String showCategoryItems(@PathVariable Long categoryId, Model model) {
        model.addAttribute("category", categoryService.getByIdAndIsActiveOrThrow(categoryId));
        model.addAttribute("items", menuItemService.findByCategoryAndIsActive(categoryId));
        return "menu/items";
    }
}