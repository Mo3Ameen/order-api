package io.everyonecodes.order_api.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.exception.InvalidOrderRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CheckoutSessionExpirer {

    public void expireOpenSession(Order order) {
        String sessionId = order.getStripeSessionId();
        if (sessionId == null) {
            return;
        }
        try {
            Session session = Session.retrieve(sessionId);
            if ("complete".equals(session.getStatus())) {
                throw new InvalidOrderRequestException("Order " + order.getId() + " is being paid and can no longer be changed.");
            }
            if ("open".equals(session.getStatus())) {
                session.expire();
            }
            order.setStripeSessionId(null);
        } catch (StripeException e) {
            log.warn("Could not expire session {} for order {} — leaving it, the payment step deals with it", sessionId, order.getId());
        }
    }
}