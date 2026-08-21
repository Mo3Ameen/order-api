package io.everyonecodes.order_api.web;

import com.stripe.exception.StripeException;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.exception.OrderAlreadyPaidException;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.payment.PaymentService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/checkout")
public class CheckoutPageController {

    private final CartSession cartSession;
    private final PaymentService paymentService;

    public CheckoutPageController(CartSession cartSession, PaymentService paymentService) {
        this.cartSession = cartSession;
        this.paymentService = paymentService;
    }

    @GetMapping
    public String getCheckout(HttpSession session, Model model) {
        Order order = cartSession.resolve(session);
        if (order == null || order.getOrderedItems().isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("order", order);
        return "checkout/checkout";
    }

    @PostMapping
    public String checkout(HttpSession session, Model model) {
        Order order = cartSession.resolve(session);
        if (order == null || order.getOrderedItems().isEmpty()) {
            return "redirect:/cart";
        }
        String url;
        try {
            url = paymentService.initiateOrderPayment(order.getId(), null);
        } catch (StripeException e) {
            log.error("Checkout for order #{} could not complete", order.getId(), e);
            model.addAttribute("order", order);
            model.addAttribute("error", "Payment could not be started. Please try again.");
            return "checkout/checkout";
        } catch (OrderAlreadyPaidException | ResourceNotFoundException e) {
            log.warn("Checkout for order #{} stopped — it is no longer payable: {}", order.getId(), e.getMessage());
            return "redirect:/cart";
        }
        return "redirect:" + url;
    }

    @GetMapping("/success")
    public String getSuccessPage() {
        return "checkout/success";
    }

    @GetMapping("/failure")
    public String getFailurePage() {
        return "checkout/failure";
    }
}
