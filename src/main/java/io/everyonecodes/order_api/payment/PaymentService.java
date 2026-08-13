package io.everyonecodes.order_api.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.exception.OrderAlreadyPaidException;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private final OrderService orderService;
    private final String webhookSecret;

    public PaymentService(OrderService orderService, @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.orderService = orderService;
        this.webhookSecret = webhookSecret;
    }

    public String initiateOrderPayment(Long orderId, String email) throws StripeException {
        Order order = orderService.getValidOrder(orderId);
        order.setCustomerEmail(email);
        return getCheckoutSession(order, "http://localhost:8080/api/payment/success", "http://localhost:8080/api/payment/failure");
    }

    public void handleWebhookEvent(String payload, String sigHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        if ("checkout.session.completed".equals(event.getType())) {

            Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
            if (stripeObject.isEmpty()) {
                log.error("Could not deserialize {} event {} — likely a Stripe API version mismatch between the account and stripe-java", event.getType(), event.getId());
                return;
            }

            Session session = (Session) stripeObject.get();

            if (!"paid".equals(session.getPaymentStatus())) {
                log.info("Ignoring checkout session {} — payment_status is '{}', not 'paid'", session.getId(), session.getPaymentStatus());
                return;
            }

            Map<String, String> metadata = session.getMetadata();
            String rawOrderId = (metadata == null) ? null : metadata.get("orderId");
            if (rawOrderId == null) {
                log.info("Ignoring checkout session {} — no orderId metadata, not created by this application", session.getId());
                return;
            }
            Long orderId = Long.valueOf(rawOrderId);

            String email = (session.getCustomerDetails() == null) ? null : session.getCustomerDetails().getEmail();
            if (email == null || email.isBlank()) {
                log.error("Checkout session {} for order {} has no customer email — cannot mark as paid", session.getId(), orderId);
                return;
            }

            try {
                orderService.payOrder(orderId, email);
                log.info("Order {} marked as paid from session {}", orderId, session.getId());
            } catch (OrderAlreadyPaidException ignore) {
                log.info("Order {} already paid — duplicate delivery of session {}", orderId, session.getId());
            } catch (ResourceNotFoundException e) {
                log.error("Checkout session {} references order {}, which no longer exists", session.getId(), orderId);
            }
        }
    }

    public String getCheckoutSession(Order order, String successUrl, String failureUrl) throws StripeException {
        log.info("Creating session for order with Id: {}", order.getId());

        SessionCreateParams sessionCreateParams = SessionCreateParams.builder()
                .setCustomerEmail(order.getCustomerEmail())
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setSuccessUrl(successUrl)
                .setCancelUrl(failureUrl)
                .addLineItem(SessionCreateParams.LineItem
                        .builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem
                                .PriceData
                                .builder()
                                .setCurrency("eur")
                                .setUnitAmount(order.getTotalPrice()
                                        .multiply(BigDecimal.valueOf(100))
                                        .longValue())
                                .setProductData(SessionCreateParams.LineItem
                                        .PriceData
                                        .ProductData
                                        .builder()
                                        .setName("Order #" + order.getId())
                                        .build())
                                .build())
                        .build())
                .putMetadata("orderId", String.valueOf(order.getId()))
                .build();

        Session session = Session.create(sessionCreateParams);

        orderService.save(order);
        log.info("Session created successfully for order with Id : {}", order.getId());
        return session.getUrl();
    }
}