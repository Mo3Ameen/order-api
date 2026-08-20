package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping("/cart")
public class CartPageController {

    private static final String CART_SESSION_KEY = "cartOrderId";
    private final OrderService orderService;

    public CartPageController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping()
    public String showCart(HttpSession session, Model model) {
        Order order = resolveCart(session);
        model.addAttribute("order", order);
        return "cart/cart";
    }

    @PostMapping("/items")
    public String addItemToCart(HttpSession session, @RequestParam Long menuItemId, @RequestParam Integer quantity, @RequestParam(required = false) Set<Long> extraIds) {
        Order resolved = resolveCart(session);
        Order order = resolved != null ? resolved : orderService.createOrder();
        session.setAttribute(CART_SESSION_KEY, order.getId());
        orderService.addItemToOrder(order.getId(), menuItemId, quantity, extraIds);
        return "redirect:/cart";
    }

    @PostMapping("/items/{orderedItemId}/quantity")
    public String updateQuantity(HttpSession session, @PathVariable Long orderedItemId, @RequestParam Integer quantity) {
        Order order = resolveCart(session);
        if (order == null) {
            return "redirect:/cart";
        }
        orderService.updateQuantity(order.getId(), orderedItemId, quantity);
        return "redirect:/cart";
    }

    private Order resolveCart(HttpSession session) {
        Long cartOrderId = (Long) session.getAttribute(CART_SESSION_KEY);
        if (cartOrderId == null) {
            return null;
        }
        return orderService.findById(cartOrderId)
                .filter(order -> !order.getIsPaid())
                .orElse(null);
    }
}