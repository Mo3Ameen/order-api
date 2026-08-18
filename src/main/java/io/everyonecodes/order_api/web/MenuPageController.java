package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuPageController {
    
    private final CategoryService categoryService;

    public MenuPageController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/menu")
    public String showCategories(Model model) {
        model.addAttribute("categories", categoryService.findAllActive());
        return "menu/categories";
    }
}
