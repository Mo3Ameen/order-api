package io.everyonecodes.order_api.payment;

import com.stripe.exception.StripeException;
import jakarta.validation.constraints.Email;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}")
    public String initiateOrderPayment(@PathVariable Long orderId, @RequestParam @Email String email) throws StripeException {
        return paymentService.initiateOrderPayment(orderId, email);
    }

    @GetMapping("/success")
    public String paymentSuccess() {
        return "Payment received. Your order is confirmed";
    }

    @GetMapping("/failure")
    public String paymentFailure() {
        return "Payment was cancelled or failed. Your order has not been paid.";
    }
}