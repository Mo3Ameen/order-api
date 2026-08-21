package io.everyonecodes.order_api.web;

import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class CartSession {

    private static final String CART_SESSION_KEY = "cartOrderId";
    private final OrderService orderService;

    public CartSession(OrderService orderService) {
        this.orderService = orderService;
    }

    public Order resolve(HttpSession session) {
        Long cartOrderId = (Long) session.getAttribute(CART_SESSION_KEY);
        if (cartOrderId == null) {
            return null;
        }
        return orderService.findById(cartOrderId)
                .filter(order -> !order.getIsPaid())
                .orElse(null);
    }

    public Order resolveOrCreate(HttpSession session) {
        Order resolved = resolve(session);
        Order order = resolved != null ? resolved : orderService.createOrder();
        session.setAttribute(CART_SESSION_KEY, order.getId());
        return order;
    }
}