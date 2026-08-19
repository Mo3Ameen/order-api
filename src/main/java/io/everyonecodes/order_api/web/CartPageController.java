package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartPageController {

    private static final String CART_SESSION_KEY = "cartOrderId";
    private final OrderService orderService;

    public CartPageController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/cart")
    public String showCart(HttpSession session, Model model) {
        Order order = resolveCart(session);
        model.addAttribute("order", order);
        return "cart/cart";
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
