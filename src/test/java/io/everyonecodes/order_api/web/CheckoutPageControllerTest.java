package io.everyonecodes.order_api.web;

import com.stripe.exception.ApiConnectionException;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.exception.OrderAlreadyPaidException;
import io.everyonecodes.order_api.payment.PaymentService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CheckoutPageControllerTest {

    private static final String CART_SESSION_KEY = "cartOrderId";
    private static final String STRIPE_URL = "https://checkout.stripe.com/c/pay/cs_test_fake";

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

    /*
     * PaymentService talks to the real Stripe API. Left unmocked, every run of the POST tests
     * would create live checkout sessions against the key in application.properties.
     */
    @MockitoBean
    protected PaymentService paymentService;

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

    private Order createPaidCart() {
        Order cart = createCartWithOneItem();
        cart.setIsPaid(true);
        return orderRepository.save(cart);
    }

    private MockHttpSession sessionHolding(Long cartOrderId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CART_SESSION_KEY, cartOrderId);
        return session;
    }

    // ---------- GET /checkout ----------

    @Test
    void getCheckout_redirectsToCart_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(get("/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    void getCheckout_redirectsToCart_whenTheCartHasNoItems() throws Exception {
        Order emptyCart = orderService.createOrder();

        mockMvc.perform(get("/checkout")
                        .sessionAttr(CART_SESSION_KEY, emptyCart.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    void getCheckout_rendersTheConfirmPage_whenTheCartHasItems() throws Exception {
        Order cart = createCartWithOneItem();

        var modelAndView = mockMvc.perform(get("/checkout")
                        .sessionAttr(CART_SESSION_KEY, cart.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/checkout"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        var order = (Order) modelAndView.getModel().get("order");
        assertNotNull(order, "the confirm page needs the order to show the summary");
        assertEquals(cart.getId(), order.getId());
    }

    // ---------- POST /checkout ----------

    @Test
    void checkout_redirectsToStripe_andSendsNoEmail() throws Exception {
        Order cart = createCartWithOneItem();
        when(paymentService.initiateOrderPayment(anyLong(), any())).thenReturn(STRIPE_URL);

        mockMvc.perform(post("/checkout")
                        .sessionAttr(CART_SESSION_KEY, cart.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(STRIPE_URL));

        verify(paymentService).initiateOrderPayment(cart.getId(), null);
    }

    @Test
    void checkout_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
        Order cart = createCartWithOneItem();

        mockMvc.perform(post("/checkout")
                        .sessionAttr(CART_SESSION_KEY, cart.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkout_redirectsToCart_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(post("/checkout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    @Test
    void checkout_rendersTheConfirmPageWithAnError_whenStripeFails() throws Exception {
        Order cart = createCartWithOneItem();
        when(paymentService.initiateOrderPayment(anyLong(), any()))
                .thenThrow(new ApiConnectionException("stripe is unreachable"));

        var modelAndView = mockMvc.perform(post("/checkout")
                        .sessionAttr(CART_SESSION_KEY, cart.getId())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/checkout"))
                .andReturn()
                .getModelAndView();

        assertNotNull(modelAndView);
        assertNotNull(modelAndView.getModel().get("error"), "the customer must be told the payment did not start");
        assertNotNull(modelAndView.getModel().get("order"), "the summary must survive the re-render");
    }

    @Test
    void checkout_redirectsToCart_whenTheOrderIsNoLongerPayable() throws Exception {
        Order cart = createCartWithOneItem();
        when(paymentService.initiateOrderPayment(anyLong(), any()))
                .thenThrow(new OrderAlreadyPaidException("Order " + cart.getId() + " is already paid!"));

        mockMvc.perform(post("/checkout")
                        .sessionAttr(CART_SESSION_KEY, cart.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));
    }

    // ---------- GET /checkout/success ----------

    @Test
    void successPage_clearsTheSessionCart_whenTheOrderIsPaid() throws Exception {
        Order paidCart = createPaidCart();
        MockHttpSession session = sessionHolding(paidCart.getId());

        mockMvc.perform(get("/checkout/success").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));

        assertNull(session.getAttribute(CART_SESSION_KEY), "a paid cart must be forgotten");
    }

    @Test
    void successPage_keepsTheSessionCart_whenTheOrderIsNotPaidYet() throws Exception {
        Order cart = createCartWithOneItem();
        MockHttpSession session = sessionHolding(cart.getId());

        mockMvc.perform(get("/checkout/success").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));

        assertEquals(cart.getId(), session.getAttribute(CART_SESSION_KEY),
                "the webhook may not have landed yet, so the cart must not be thrown away");
    }

    @Test
    void successPage_rendersWithoutFailing_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(get("/checkout/success"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));
    }

    @Test
    void successPage_asksStripeToConfirmTheOrder() throws Exception {
        Order cart = createCartWithOneItem();

        mockMvc.perform(get("/checkout/success").session(sessionHolding(cart.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));

        verify(paymentService).confirmPaymentFromStripe(cart.getId());
    }

    @Test
    void successPage_clearsTheSessionCart_whenStripeConfirmsThePayment() throws Exception {
        Order cart = createCartWithOneItem();
        MockHttpSession session = sessionHolding(cart.getId());

        /*
         * Stands in for the reconcile marking the order paid. The cart may only be cleared
         * after that has happened, so this fails if clearIfPaid runs before the confirmation.
         */
        doAnswer(invocation -> {
            Order confirmed = orderRepository.findById(cart.getId()).orElseThrow();
            confirmed.setIsPaid(true);
            orderRepository.save(confirmed);
            return null;
        }).when(paymentService).confirmPaymentFromStripe(cart.getId());

        mockMvc.perform(get("/checkout/success").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));

        assertNull(session.getAttribute(CART_SESSION_KEY),
                "the reconcile paid the order, so the cart must be forgotten");
    }

    @Test
    void successPage_rendersAndKeepsTheCart_whenStripeIsUnreachable() throws Exception {
        Order cart = createCartWithOneItem();
        MockHttpSession session = sessionHolding(cart.getId());
        doThrow(new ApiConnectionException("stripe is unreachable"))
                .when(paymentService).confirmPaymentFromStripe(anyLong());

        mockMvc.perform(get("/checkout/success").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/success"));

        assertEquals(cart.getId(), session.getAttribute(CART_SESSION_KEY),
                "payment could not be confirmed, so the cart must survive");
    }

    @Test
    void successPage_doesNotAskStripe_whenSessionHasNoCart() throws Exception {
        mockMvc.perform(get("/checkout/success"))
                .andExpect(status().isOk());

        verify(paymentService, never()).confirmPaymentFromStripe(anyLong());
    }

    @Test
    void successPage_doesNotAskStripe_whenTheOrderIsAlreadyPaid() throws Exception {
        Order paidCart = createPaidCart();

        mockMvc.perform(get("/checkout/success").session(sessionHolding(paidCart.getId())))
                .andExpect(status().isOk());

        verify(paymentService, never()).confirmPaymentFromStripe(anyLong());
    }

    // ---------- GET /checkout/failure ----------

    @Test
    void failurePage_rendersAndKeepsTheSessionCart() throws Exception {
        Order cart = createCartWithOneItem();
        MockHttpSession session = sessionHolding(cart.getId());

        mockMvc.perform(get("/checkout/failure").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/failure"));

        assertEquals(cart.getId(), session.getAttribute(CART_SESSION_KEY),
                "a cancelled payment must leave the cart untouched");
    }
}
