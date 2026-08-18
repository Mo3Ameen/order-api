package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.AddItemRequestDto;
import io.everyonecodes.order_api.dto.KitchenTicketDto;
import io.everyonecodes.order_api.entity.*;
import io.everyonecodes.order_api.repository.CategoryRepository;
import io.everyonecodes.order_api.repository.ExtraRepository;
import io.everyonecodes.order_api.repository.MenuItemRepository;
import io.everyonecodes.order_api.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureRestTestClient
@Transactional
@AutoConfigureMockMvc
class OrderControllerTest {
    @Autowired
    protected RestTestClient restTestClient;
    @Autowired
    protected OrderRepository orderRepository;
    @Autowired
    protected MenuItemRepository menuItemRepository;
    @Autowired
    protected CategoryRepository categoryRepository;
    @Autowired
    protected ExtraRepository extraRepository;

    protected MenuItem activeMenuItem;
    protected MenuItem inactiveMenuItem;
    protected Extra activeExtra;
    protected Extra secondActiveExtra;
    protected Extra inactiveExtra;
    protected Extra unlinkedExtra;

    @BeforeEach
    void setUpFixtures() {
        var category = new Category();
        category.setName("Burgers");
        category.setIsActive(true);
        category = categoryRepository.save(category);

        activeMenuItem = new MenuItem();
        activeMenuItem.setName("Cheeseburger");
        activeMenuItem.setDescription("Beef burger");
        activeMenuItem.setPrice(BigDecimal.valueOf(10));
        activeMenuItem.setIsActive(true);
        activeMenuItem.setCategory(category);
        activeMenuItem = menuItemRepository.save(activeMenuItem);

        inactiveMenuItem = new MenuItem();
        inactiveMenuItem.setName("Discontinued Burger");
        inactiveMenuItem.setDescription("No longer sold");
        inactiveMenuItem.setPrice(BigDecimal.valueOf(8));
        inactiveMenuItem.setIsActive(false);
        inactiveMenuItem.setCategory(category);
        inactiveMenuItem = menuItemRepository.save(inactiveMenuItem);

        activeExtra = new Extra();
        activeExtra.setName("Extra Cheese");
        activeExtra.setPrice(BigDecimal.valueOf(1.5));
        activeExtra.setIsActive(true);
        activeExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(activeExtra);
        activeExtra = extraRepository.save(activeExtra);

        secondActiveExtra = new Extra();
        secondActiveExtra.setName("Bacon");
        secondActiveExtra.setPrice(BigDecimal.valueOf(2));
        secondActiveExtra.setIsActive(true);
        secondActiveExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(secondActiveExtra);
        secondActiveExtra = extraRepository.save(secondActiveExtra);

        inactiveExtra = new Extra();
        inactiveExtra.setName("Mushrooms");
        inactiveExtra.setPrice(BigDecimal.valueOf(1));
        inactiveExtra.setIsActive(false);
        inactiveExtra.getMenuItems().add(activeMenuItem);
        activeMenuItem.getExtras().add(inactiveExtra);
        inactiveExtra = extraRepository.save(inactiveExtra);

        unlinkedExtra = new Extra();
        unlinkedExtra.setName("Pickles");
        unlinkedExtra.setPrice(BigDecimal.valueOf(0.5));
        unlinkedExtra.setIsActive(true);
        unlinkedExtra = extraRepository.save(unlinkedExtra); // NOT linked to activeMenuItem
    }

    // ---------- fixture builders ----------

    protected Order createOrder(boolean isPaid) {
        var order = new Order();
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalPrice(BigDecimal.ZERO);
        order.setIsPaid(isPaid);
        order.setIsFulfilled(false);
        order.setOrderedItems(new HashSet<>());
        return orderRepository.save(order);
    }

    protected Order createOrderWithItem() {
        var order = new Order();
        order.setCreatedAt(LocalDateTime.now());
        order.setIsPaid(false);
        order.setIsFulfilled(false);
        order.setOrderedItems(new HashSet<>());

        var item = new OrderedItem();
        item.setMenuItem(activeMenuItem);
        item.setQuantity(1);
        item.setPriceAtPurchase(activeMenuItem.getPrice());
        item.setSelectedExtras(new HashSet<>());
        item.setOrder(order);

        order.getOrderedItems().add(item);
        order.setTotalPrice(activeMenuItem.getPrice());

        return orderRepository.save(order);
    }

    protected Order createOrderWithItemAndExtra() {
        var order = new Order();
        order.setCreatedAt(LocalDateTime.now());
        order.setIsPaid(false);
        order.setIsFulfilled(false);
        order.setOrderedItems(new HashSet<>());

        var item = new OrderedItem();
        item.setMenuItem(activeMenuItem);
        item.setQuantity(1);
        item.setPriceAtPurchase(activeMenuItem.getPrice());
        item.setSelectedExtras(new HashSet<>());
        item.setOrder(order);

        var selectedExtra = new SelectedExtra();
        selectedExtra.setExtra(activeExtra);
        selectedExtra.setPriceAtPurchase(activeExtra.getPrice());
        selectedExtra.setOrderedItem(item);
        item.getSelectedExtras().add(selectedExtra);

        order.getOrderedItems().add(item);
        order.setTotalPrice(activeMenuItem.getPrice().add(activeExtra.getPrice()));

        return orderRepository.save(order);
    }

    protected OrderedItem firstItem(Order order) {
        return order.getOrderedItems().stream().findFirst().orElseThrow();
    }

    // ---------- GET /api/orders/{orderId} ----------

    @Nested
    class GetOrder {

        @Test
        void returnsOrder_whenOrderExists() {
            var order = createOrder(false);

            var result = restTestClient.get()
                    .uri("/api/orders/{orderId}", order.getId())
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertEquals(order.getId(), result.getId());
            assertFalse(result.getIsPaid());
        }

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentId = 999999L;

            var result = restTestClient.get()
                    .uri("/api/orders/{orderId}", nonExistentId)
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentId + " not found", result);
        }
    }

    // ---------- GET /api/orders/kitchen ----------

    @Nested
    class FindAllPaidUnfulfilled {

        @Test
        void returnsOnlyPaidAndUnfulfilledOrders() {
            var paidUnfulfilled = createOrderWithItem();
            paidUnfulfilled.setIsPaid(true);
            paidUnfulfilled.setPaidAt(LocalDateTime.now());
            paidUnfulfilled = orderRepository.save(paidUnfulfilled);

            var unpaid = createOrderWithItem();

            var paidAndFulfilled = createOrderWithItem();
            paidAndFulfilled.setIsPaid(true);
            paidAndFulfilled.setPaidAt(LocalDateTime.now());
            paidAndFulfilled.setIsFulfilled(true);
            paidAndFulfilled = orderRepository.save(paidAndFulfilled);

            var result = getKitchenOrders()
                    .expectStatus().isOk()
                    .expectBody(KitchenTicketDto[].class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            var ids = Stream.of(result).map(KitchenTicketDto::getOrderId).toList();

            assertTrue(ids.contains(paidUnfulfilled.getId()));
            assertFalse(ids.contains(unpaid.getId()));
            assertFalse(ids.contains(paidAndFulfilled.getId()));
        }

        @Test
        void mapsOrderedItemsAndExtrasCorrectly() {
            var order = createOrderWithItemAndExtra();
            order.setIsPaid(true);
            order.setPaidAt(LocalDateTime.now());
            order = orderRepository.save(order);

            var result = getKitchenOrders()
                    .expectStatus().isOk()
                    .expectBody(KitchenTicketDto[].class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            var savedOrderId = order.getId();
            var ticket = Stream.of(result)
                    .filter(candidate -> candidate.getOrderId().equals(savedOrderId))
                    .findFirst()
                    .orElseThrow();

            assertEquals(1, ticket.getItems().size());
            var itemDto = ticket.getItems().getFirst();
            assertEquals(activeMenuItem.getName(), itemDto.getItemName());
            assertEquals(1, itemDto.getQuantity());
            assertEquals(List.of(activeExtra.getName()), itemDto.getExtraNames());
        }

        private RestTestClient.ResponseSpec getKitchenOrders() {
            return restTestClient.get()
                    .uri("/api/orders/kitchen")
                    .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                    .exchange();
        }
    }

    // ---------- PUT /api/orders/{orderId}/fulfill ----------

    @Nested
    class MarkFulfilled {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = fulfill(nonExistentOrderId)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns400_whenOrderIsNotPaid() {
            var order = createOrder(false);

            var result = fulfill(order.getId())
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is not paid yet.", result);
        }

        @Test
        void passesWhenOrderIsPaidAndUnfulfilled() {
            var order = createOrder(true);

            var result = fulfill(order.getId())
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.getIsFulfilled());
        }

        @Test
        void isIdempotent_whenOrderIsAlreadyFulfilled() {
            var order = createOrder(true);

            fulfill(order.getId()).expectStatus().isOk();

            var result = fulfill(order.getId())
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.getIsFulfilled());
        }

        private RestTestClient.ResponseSpec fulfill(Long orderId) {
            return restTestClient
                    .put()
                    .uri("/api/orders/{orderId}/fulfill", orderId)
                    .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                    .exchange();
        }
    }

    // ---------- POST /api/orders ----------

    @Nested
    class PostOrder {

        @Test
        void createsOrder() {
            var result = restTestClient.post()
                    .uri("/api/orders")
                    .exchange()
                    .expectStatus()
                    .isCreated()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertNotNull(result.getId());
            assertNotNull(result.getCreatedAt());
            assertFalse(result.getIsPaid());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalPrice()));
        }
    }

    // ---------- POST /api/orders/{orderId}/items ----------

    @Nested
    class AddItemToOrder {

        @Test
        void returns400_whenQuantityIsZero() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 0, null);

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.contains("Quantity must be at least 1"));
        }

        @Test
        void returns400_whenQuantityIsNull() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), null, null);

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Quantity can not be null or less than 1", result);
        }

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, null);

            var result = post(nonExistentOrderId, body)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, null);

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns404_whenMenuItemDoesNotExist() {
            var order = createOrder(false);
            Long nonExistentMenuItemId = 999999L;
            var body = new AddItemRequestDto(nonExistentMenuItemId, 1, null);

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("MenuItem " + nonExistentMenuItemId + " not found", result);
        }

        @Test
        void returns400_whenMenuItemIsNotActive() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(inactiveMenuItem.getId(), 1, null);

            var result = post(order.getId(), body).expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Menu item not active: " + inactiveMenuItem.getId(), result);
        }

        @Test
        void returns404_whenExtraDoesNotExist() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, Set.of(999999L));

            var result = post(order.getId(), body).expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("One or more extras were not found", result);
        }

        @Test
        void returns400_whenExtraIsNotActive() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, Set.of(inactiveExtra.getId()));

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Extra " + inactiveExtra.getId() + " is not valid for this item or is not active.", result);
        }

        @Test
        void returns400_whenExtraIsNotValidForThisItem() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, Set.of(unlinkedExtra.getId()));

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Extra " + unlinkedExtra.getId() + " is not valid for this item or is not active.", result);
        }

        @Test
        void passesWithoutExtras() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 2, null);

            var result = post(order.getId(), body)
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertEquals(1, result.getOrderedItems().size());
            assertEquals(0, BigDecimal.valueOf(20).compareTo(result.getTotalPrice())); // 10 * 2
        }

        @Test
        void passesWithExtras() {
            var order = createOrder(false);
            var body = new AddItemRequestDto(activeMenuItem.getId(), 1, Set.of(activeExtra.getId()));

            var result = post(order.getId(), body).expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertEquals(1, result.getOrderedItems().size());
            assertEquals(0, BigDecimal.valueOf(11.5).compareTo(result.getTotalPrice())); // 10 + 1.5
        }

        private RestTestClient.ResponseSpec post(Long orderId, AddItemRequestDto body) {
            return restTestClient
                    .post()
                    .uri("/api/orders/{orderId}/items", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange();
        }
    }

    // ---------- PUT /api/orders/{orderId}/items/{itemId} ----------

    @Nested
    class UpdateQuantity {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = updateQuantity(nonExistentOrderId, 1L, 5)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);

            var result = updateQuantity(order.getId(), 1L, 5)
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns404_whenOrderedItemDoesNotExist() {
            var order = createOrder(false);
            Long nonExistentItemId = 999999L;

            var result = updateQuantity(order.getId(), nonExistentItemId, 5)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Ordered item " + nonExistentItemId + " not found in this order", result);
        }

        @Test
        void returns400_whenQuantityIsZero() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = updateQuantity(order.getId(), item.getId(), 0)
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Quantity can not be null or less than 1", result);
        }

        @Test
        void passesWhenAllGood() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = updateQuantity(order.getId(), item.getId(), 3)
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            var savedItem = firstItem(result);
            assertEquals(3, savedItem.getQuantity());
            assertEquals(0, BigDecimal.valueOf(30).compareTo(result.getTotalPrice())); // 10 * 3
        }

        private RestTestClient.ResponseSpec updateQuantity(Long orderId, Long itemId, Integer quantity) {
            return restTestClient
                    .put()
                    .uri("/api/orders/{orderId}/items/{itemId}?quantity={quantity}", orderId, itemId, quantity)
                    .exchange();
        }
    }

    // ---------- DELETE /api/orders/{orderId}/items/{itemId} ----------

    @Nested
    class RemoveItemFromOrder {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = delete(nonExistentOrderId, 1L)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);

            var result = delete(order.getId(), 1L).expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns404_whenItemDoesNotExist() {
            var order = createOrder(false);
            Long nonExistentItemId = 999999L;

            var result = delete(order.getId(), nonExistentItemId)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Ordered item " + nonExistentItemId + " not found in this order", result);
        }

        @Test
        void passesWhenAllGood() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = delete(order.getId(), item.getId())
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.getOrderedItems().isEmpty());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalPrice()));
        }

        private RestTestClient.ResponseSpec delete(Long orderId, Long itemId) {
            return restTestClient
                    .delete()
                    .uri("/api/orders/{orderId}/items/{itemId}", orderId, itemId)
                    .exchange();
        }
    }

    // ---------- POST /api/orders/{orderId}/items/{itemId}/extras/{extraId} ----------

    @Nested
    class AddExtra {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = addExtra(nonExistentOrderId, 1L, 1L)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);

            var result = addExtra(order.getId(), 1L, 1L)
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns404_whenOrderedItemDoesNotExist() {
            var order = createOrder(false);
            Long nonExistentItemId = 999999L;

            var result = addExtra(order.getId(), nonExistentItemId, 1L)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class).returnResult()
                    .getResponseBody();

            assertEquals("Ordered item " + nonExistentItemId + " not found in this order", result);
        }

        @Test
        void returns404_whenExtraDoesNotExist() {
            var order = createOrderWithItem();
            var item = firstItem(order);
            Long nonExistentExtraId = 999999L;

            var result = addExtra(order.getId(), item.getId(), nonExistentExtraId)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Extra " + nonExistentExtraId + " not found", result);
        }

        @Test
        void returns400_whenExtraIsNotActive() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = addExtra(order.getId(), item.getId(), inactiveExtra.getId())
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Extra " + inactiveExtra.getId() + " is not active", result);
        }

        @Test
        void returns400_whenExtraIsNotValidForThisItem() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = addExtra(order.getId(), item.getId(), unlinkedExtra.getId())
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("You can not add this extra to this item", result);
        }

        @Test
        void returns400_whenExtraAlreadyAdded() {
            var order = createOrderWithItemAndExtra();
            var item = firstItem(order);

            var result = addExtra(order.getId(), item.getId(), activeExtra.getId())
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Extra " + activeExtra.getId() + " is already added to this item", result);
        }

        @Test
        void passesWhenAllGood() {
            var order = createOrderWithItem();
            var item = firstItem(order);

            var result = addExtra(order.getId(), item.getId(), secondActiveExtra.getId())
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            var savedItem = firstItem(result);
            assertEquals(1, savedItem.getSelectedExtras().size());
            assertEquals(0, BigDecimal.valueOf(12).compareTo(result.getTotalPrice())); // 10 + 2
        }

        private RestTestClient.ResponseSpec addExtra(Long orderId, Long itemId, Long extraId) {
            return restTestClient
                    .post()
                    .uri("/api/orders/{orderId}/items/{itemId}/extras/{extraId}", orderId, itemId, extraId)
                    .exchange();
        }
    }

    // ---------- DELETE /api/orders/{orderId}/items/{itemId}/extras/{selectedExtraId} ----------

    @Nested
    class RemoveExtra {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = removeExtra(nonExistentOrderId, 1L, 1L)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);

            var result = removeExtra(order.getId(), 1L, 1L)
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns404_whenOrderedItemDoesNotExist() {
            var order = createOrder(false);
            Long nonExistentItemId = 999999L;

            var result = removeExtra(order.getId(), nonExistentItemId, 1L)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Ordered item " + nonExistentItemId + " not found in this order", result);
        }

        @Test
        void returns404_whenSelectedExtraDoesNotExist() {
            var order = createOrderWithItem();
            var item = firstItem(order);
            Long nonExistentSelectedExtraId = 999999L;

            var result = removeExtra(order.getId(), item.getId(), nonExistentSelectedExtraId)
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Selected extra " + nonExistentSelectedExtraId + " not found", result);
        }

        @Test
        void passesWhenAllGood() {
            var order = createOrderWithItemAndExtra();
            var item = firstItem(order);
            var selectedExtra = item.getSelectedExtras().stream().findFirst().orElseThrow();

            var result = removeExtra(order.getId(), item.getId(), selectedExtra.getId())
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            var savedItem = firstItem(result);
            assertTrue(savedItem.getSelectedExtras().isEmpty());
            assertEquals(0, BigDecimal.valueOf(10).compareTo(result.getTotalPrice()));
        }

        private RestTestClient.ResponseSpec removeExtra(Long orderId, Long itemId, Long selectedExtraId) {
            return restTestClient.delete()
                    .uri("/api/orders/{orderId}/items/{itemId}/extras/{selectedExtraId}", orderId, itemId, selectedExtraId)
                    .exchange();
        }
    }

    // ---------- POST /api/orders/{orderId}/pay ----------

    @Nested
    class PayOrder {

        @Test
        void returns404_whenOrderDoesNotExist() {
            Long nonExistentOrderId = 999999L;

            var result = pay(nonExistentOrderId, "customer@example.com")
                    .expectStatus()
                    .isNotFound()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + nonExistentOrderId + " not found", result);
        }

        @Test
        void returns409_whenOrderIsAlreadyPaid() {
            var order = createOrder(true);

            var result = pay(order.getId(), "customer@example.com")
                    .expectStatus()
                    .isEqualTo(HttpStatus.CONFLICT)
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Order " + order.getId() + " is already paid!", result);
        }

        @Test
        void returns400_whenOrderIsEmpty() {
            var order = createOrder(false);

            var result = pay(order.getId(), "customer@example.com")
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Cannot pay for an empty order: " + order.getId(), result);
        }

        @Test
        void returns400_whenEmailIsBlank() {
            var order = createOrderWithItem();

            var result = pay(order.getId(), "")
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertEquals("Customer email is required to complete payment.", result);
        }

        @Test
        void returns400_whenEmailIsMalformed() {
            var order = createOrderWithItem();

            var result = pay(order.getId(), "not-an-email")
                    .expectStatus()
                    .isBadRequest()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.toLowerCase().contains("email"));
        }

        @Test
        void passesWhenAllGood() {
            var order = createOrderWithItem();

            var result = pay(order.getId(), "customer@example.com")
                    .expectStatus()
                    .isOk()
                    .expectBody(Order.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(result);
            assertTrue(result.getIsPaid());
            assertEquals("customer@example.com", result.getCustomerEmail());
        }

        @Test
        void returns401_whenNotAuthenticated() {
            var order = createOrderWithItem();

            restTestClient
                    .post()
                    .uri("/api/orders/{orderId}/pay?email={email}", order.getId(), "customer@example.com")
                    .exchange()
                    .expectStatus()
                    .isUnauthorized();
        }

        private RestTestClient.ResponseSpec pay(Long orderId, String email) {
            return restTestClient
                    .post()
                    .uri("/api/orders/{orderId}/pay?email={email}", orderId, email)
                    .headers(headers -> headers.setBasicAuth("admin", "admin123"))
                    .exchange();
        }
    }
}