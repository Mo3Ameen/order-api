package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.entity.OrderedItem;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import io.everyonecodes.order_api.repository.OrderRepository;
import io.everyonecodes.order_api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CartPageControllerTest {

    private static final String CART_SESSION_KEY = "cartOrderId";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected MenuItemRepository menuItemRepository;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected OrderService orderService;

    protected MenuItem activeMenuItem;

    @BeforeEach
    void setUpFixtures() {
        Category activeCategory = new Category();
        activeCategory.setName("Burgers");
        activeCategory.setIsActive(true);
        activeCategory = categoryRepository.save(activeCategory);

        activeMenuItem = new MenuItem();
        activeMenuItem.setName("Cheeseburger");
        activeMenuItem.setDescription("Beef burger");
        activeMenuItem.setPrice(BigDecimal.valueOf(10));
        activeMenuItem.setIsActive(true);
        activeMenuItem.setCategory(activeCategory);
        activeMenuItem = menuItemRepository.save(activeMenuItem);
    }

    private Order createCartWithOneItem() {
        Order order = orderService.createOrder();
        return orderService.addItemToOrder(order.getId(), activeMenuItem.getId(), 1, null);
    }

    private OrderedItem onlyItemOf(Order order) {
        return order.getOrderedItems().iterator().next();
    }

    // ---------- GET /cart ----------

    @Test
    void showCart_rendersCartViewWithNullOrder_whenSessionHasNoCart() throws Exception {
        var modelAndView = mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        assertNull(modelAndView.getModel().get("order"), "a browser with no session should see an empty cart");
    }

    @Test
    void showCart_rendersTheOrder_whenSessionHasAnUnpaidCart() throws Exception {
        Order cart = createCartWithOneItem();

        var modelAndView = mockMvc.perform(get("/cart")
                        .sessionAttr(CART_SESSION_KEY, cart.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("cart/cart"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        var order = (Order) modelAndView.getModel().get("order");
        assertNotNull(order);
        assertEquals(cart.getId(), order.getId());
    }

    @Test
    void showCart_rendersNullOrder_whenSessionCartIsAlreadyPaid() throws Exception {
        Order cart = createCartWithOneItem();
        cart.setIsPaid(true);
        orderRepository.save(cart);

        var modelAndView = mockMvc.perform(get("/cart")
                        .sessionAttr(CART_SESSION_KEY, cart.getId()))
                .andExpect(status().isOk())
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        assertNull(modelAndView.getModel().get("order"), "a paid order is no longer a cart");
    }

    @Test
    void showCart_rendersNullOrder_whenSessionCartNoLongerExists() throws Exception {
        var modelAndView = mockMvc.perform(get("/cart")
                        .sessionAttr(CART_SESSION_KEY, 999999L))
                .andExpect(status().isOk())
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        assertNull(modelAndView.getModel().get("order"), "a stale id must not blow up the page");
    }

    // ---------- POST /cart/items ----------

    @Test
    void addItemToCart_createsAnOrderAndStoresItsId_whenSessionHasNoCart() throws Exception {
        var result = mockMvc.perform(post("/cart/items")
                        .with(csrf())
                        .param("menuItemId", String.valueOf(activeMenuItem.getId()))
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andReturn();

        var cartId = (Long) Objects.requireNonNull(result.getRequest().getSession()).getAttribute(CART_SESSION_KEY);
        assertNotNull(cartId, "the new cart id must be stored in the session");

        Order cart = orderRepository.findById(cartId).orElseThrow();
        assertEquals(1, cart.getOrderedItems().size());
        assertEquals(2, onlyItemOf(cart).getQuantity());
    }

    @Test
    void addItemToCart_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .param("menuItemId", String.valueOf(activeMenuItem.getId()))
                        .param("quantity", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addItemToCart_reusesTheSameOrder_whenTheSessionAlreadyHasACart() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/cart/items")
                        .session(session)
                        .with(csrf())
                        .param("menuItemId", String.valueOf(activeMenuItem.getId()))
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andReturn();

        Long firstCartId = (Long) session.getAttribute(CART_SESSION_KEY);
        assertNotNull(firstCartId);

        mockMvc.perform(post("/cart/items")
                        .session(session)
                        .with(csrf())
                        .param("menuItemId", String.valueOf(activeMenuItem.getId()))
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"))
                .andReturn();

        Long secondCartId = (Long) session.getAttribute(CART_SESSION_KEY);
        assertNotNull(secondCartId);

        assertEquals(firstCartId, secondCartId);

        var order = orderRepository.findById(firstCartId).orElseThrow();
        assertEquals(2, order.getOrderedItems().size());
    }


    // ---------- POST /cart/items/{orderedItemId}/quantity ----------

    @Test
    void updateQuantity_changesTheQuantityAndRedirects() throws Exception {
        Order cart = createCartWithOneItem();
        Long orderedItemId = onlyItemOf(cart).getId();

        mockMvc.perform(post("/cart/items/{orderedItemId}/quantity", orderedItemId)
                        .sessionAttr(CART_SESSION_KEY, cart.getId())
                        .with(csrf())
                        .param("quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        Order reloaded = orderRepository.findById(cart.getId()).orElseThrow();
        assertEquals(5, onlyItemOf(reloaded).getQuantity());
    }

    @Test
    void updateQuantity_redirectsWithoutFailing_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(post("/cart/items/{orderedItemId}/quantity", 999999L)
                        .with(csrf())
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    // ---------- POST /cart/items/{orderedItemId}/remove ----------

    @Test
    void removeItem_removesTheRowAndRedirects() throws Exception {
        Order cart = createCartWithOneItem();
        Long orderedItemId = onlyItemOf(cart).getId();

        mockMvc.perform(post("/cart/items/{orderedItemId}/remove", orderedItemId)
                        .sessionAttr(CART_SESSION_KEY, cart.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        Order reloaded = orderRepository.findById(cart.getId()).orElseThrow();
        assertTrue(reloaded.getOrderedItems().isEmpty());
    }

    @Test
    void removeItem_redirectsWithoutFailing_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(post("/cart/items/{orderedItemId}/remove", 999999L)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }
}
