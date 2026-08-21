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

    private final OrderService orderService;
    private final CartSession cartSession;

    public CartPageController(OrderService orderService, CartSession cartSession) {
        this.orderService = orderService;
        this.cartSession = cartSession;
    }

    @GetMapping()
    public String showCart(HttpSession session, Model model) {
        Order order = cartSession.resolve(session);
        model.addAttribute("order", order);
        return "cart/cart";
    }

    @PostMapping("/items")
    public String addItemToCart(HttpSession session, @RequestParam Long menuItemId, @RequestParam Integer quantity, @RequestParam(required = false) Set<Long> extraIds) {
        Order order = cartSession.resolveOrCreate(session);
        orderService.addItemToOrder(order.getId(), menuItemId, quantity, extraIds);
        return "redirect:/cart";
    }

    @PostMapping("/items/{orderedItemId}/quantity")
    public String updateQuantity(HttpSession session, @PathVariable Long orderedItemId, @RequestParam Integer quantity) {
        Order order = cartSession.resolve(session);
        if (order == null) {
            return "redirect:/cart";
        }
        orderService.updateQuantity(order.getId(), orderedItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/items/{orderedItemId}/remove")
    public String removeItem(HttpSession session, @PathVariable Long orderedItemId) {
        Order order = cartSession.resolve(session);
        if (order == null) {
            return "redirect:/cart";
        }
        orderService.removeItemFromOrder(order.getId(), orderedItemId);
        return "redirect:/cart";
    }
}