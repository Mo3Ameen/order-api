package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class MenuPageControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected MenuItemRepository menuItemRepository;
    @Autowired
    protected ExtraRepository extraRepository;

    protected Category activeCategory;
    protected Category inactiveCategory;
    protected MenuItem activeMenuItem;
    protected MenuItem inactiveMenuItem;
    protected Extra activeExtra;
    protected Extra inactiveExtra;

    @BeforeEach
    void setUpFixtures() {
        activeCategory = new Category();
        activeCategory.setName("Burgers");
        activeCategory.setIsActive(true);
        activeCategory = categoryRepository.save(activeCategory);

        inactiveCategory = new Category();
        inactiveCategory.setName("Discontinued Category");
        inactiveCategory.setIsActive(false);
        inactiveCategory = categoryRepository.save(inactiveCategory);

        activeMenuItem = new MenuItem();
        activeMenuItem.setName("Cheeseburger");
        activeMenuItem.setDescription("Beef burger");
        activeMenuItem.setPrice(BigDecimal.valueOf(10));
        activeMenuItem.setIsActive(true);
        activeMenuItem.setCategory(activeCategory);
        activeMenuItem = menuItemRepository.save(activeMenuItem);

        inactiveMenuItem = new MenuItem();
        inactiveMenuItem.setName("Discontinued Burger");
        inactiveMenuItem.setDescription("No longer sold");
        inactiveMenuItem.setPrice(BigDecimal.valueOf(8));
        inactiveMenuItem.setIsActive(false);
        inactiveMenuItem.setCategory(activeCategory);
        inactiveMenuItem = menuItemRepository.save(inactiveMenuItem);

        activeExtra = new Extra();
        activeExtra.setName("Extra Cheese");
        activeExtra.setPrice(BigDecimal.valueOf(1.5));
        activeExtra.setIsActive(true);
        activeExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(activeExtra);
        activeExtra = extraRepository.save(activeExtra);

        inactiveExtra = new Extra();
        inactiveExtra.setName("Mushrooms");
        inactiveExtra.setPrice(BigDecimal.valueOf(1));
        inactiveExtra.setIsActive(false);
        inactiveExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(inactiveExtra);
        inactiveExtra = extraRepository.save(inactiveExtra);
    }

    // ---------- GET /menu ----------

    @Test
    void showCategories_rendersCategoriesViewWithCategoriesAttribute() throws Exception {
        var modelAndView = mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu/categories"))
                .andExpect(model().attributeExists("categories"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        @SuppressWarnings("unchecked")
        var categories = (List<Category>) modelAndView.getModel().get("categories");

        var names = categories.stream().map(Category::getName).toList();
        assertTrue(names.contains(activeCategory.getName()));
        assertFalse(names.contains(inactiveCategory.getName()));
    }

    // ---------- GET /menu/categories/{categoryId} ----------

    @Test
    void showCategoryItems_rendersItemsViewWithCategoryAndItemsAttributes() throws Exception {
        var modelAndView = mockMvc.perform(get("/menu/categories/{categoryId}", activeCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("menu/items"))
                .andExpect(model().attributeExists("category"))
                .andExpect(model().attributeExists("items"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        var category = (Category) modelAndView.getModel().get("category");
        assertEquals(activeCategory.getId(), category.getId());

        @SuppressWarnings("unchecked")
        var items = (List<MenuItem>) modelAndView.getModel().get("items");

        var names = items.stream().map(MenuItem::getName).toList();
        assertTrue(names.contains(activeMenuItem.getName()));
        assertFalse(names.contains(inactiveMenuItem.getName()));
    }

    @Test
    void showCategoryItems_returnsNotFound_whenCategoryIsInactive() throws Exception {
        mockMvc.perform(get("/menu/categories/{categoryId}", inactiveCategory.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void showCategoryItems_returnsNotFound_whenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(get("/menu/categories/{categoryId}", 999999L))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /menu/items/{menuItemId} ----------

    @Test
    void showItemDetails_rendersItemDetailViewWithItemAndExtrasAttributes() throws Exception {
        var modelAndView = mockMvc.perform(get("/menu/items/{menuItemId}", activeMenuItem.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("menu/item-detail"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attributeExists("extras"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        var item = (MenuItem) modelAndView.getModel().get("item");
        assertEquals(activeMenuItem.getId(), item.getId());

        @SuppressWarnings("unchecked")
        var extras = (List<Extra>) modelAndView.getModel().get("extras");

        var names = extras.stream().map(Extra::getName).toList();
        assertTrue(names.contains(activeExtra.getName()));
        assertFalse(names.contains(inactiveExtra.getName()));
    }

    @Test
    void showItemDetails_returnsNotFound_whenMenuItemIsInactive() throws Exception {
        mockMvc.perform(get("/menu/items/{menuItemId}", inactiveMenuItem.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void showItemDetails_returnsNotFound_whenMenuItemDoesNotExist() throws Exception {
        mockMvc.perform(get("/menu/items/{menuItemId}", 999999L))
                .andExpect(status().isNotFound());
    }
}
