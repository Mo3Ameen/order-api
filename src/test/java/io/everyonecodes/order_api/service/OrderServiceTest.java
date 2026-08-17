package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.*;
import io.everyonecodes.order_api.exception.InvalidOrderRequestException;
import io.everyonecodes.order_api.exception.OrderAlreadyPaidException;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MenuItemService menuItemService;
    @Mock
    private ExtraService extraService;
    @Mock
    private ReceiptService receiptService;

    private Order order;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setIsPaid(false);
        order.setOrderedItems(new HashSet<>());

        menuItem = new MenuItem();
        menuItem.setIsActive(true);
        menuItem.setId(10L);
        menuItem.setPrice(BigDecimal.valueOf(10));
        menuItem.setExtras(new HashSet<>());
    }

    @Test
    void findAll() {
        List<Order> orders = List.of(
                new Order(1L, LocalDateTime.now(), BigDecimal.valueOf(20), false, false, "", new HashSet<>(), null),
                new Order(2L, LocalDateTime.now().minusDays(3), BigDecimal.valueOf(30), true, false, "", new HashSet<>(), null),
                new Order(3L, LocalDateTime.now().minusDays(15), BigDecimal.valueOf(10), false, false, "", new HashSet<>(), null)
        );

        when(orderRepository.findAll()).thenReturn(orders);

        var result = orderService.findAll();

        List<Order> expected = List.of(
                new Order(1L, LocalDateTime.now(), BigDecimal.valueOf(20), false, false, "", new HashSet<>(), null),
                new Order(2L, LocalDateTime.now().minusDays(3), BigDecimal.valueOf(30), true, false, "", new HashSet<>(), null),
                new Order(3L, LocalDateTime.now().minusDays(15), BigDecimal.valueOf(10), false, false, "", new HashSet<>(), null)
        );

        assertEquals(expected, result);

        verify(orderRepository).findAll();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findById_withExistingOrder() {
        Long id = 1L;
        var expected = new Order(3L, LocalDateTime.now().minusDays(15), BigDecimal.valueOf(10), false, false, "", new HashSet<>(), null);

        when(orderRepository.findById(id)).thenReturn(Optional.of(expected));

        var result = orderService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());

        verify(orderRepository).findById(id);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findById_withNonExistingOrder() {
        Long id = 4L;
        Optional<Order> expected = Optional.empty();

        when(orderRepository.findById(id)).thenReturn(expected);

        var result = orderService.findById(id);

        assertFalse(result.isPresent());

        verify(orderRepository).findById(id);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void save_newOrder() {
        var newEntity = new Order();
        var expected = new Order();

        when(orderRepository.save(newEntity)).thenReturn(expected);

        var result = orderService.save(newEntity);

        assertEquals(expected, result);

        verify(orderRepository).save(newEntity);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void save_existingOrder() {
        var existingEntity = new Order();
        existingEntity.setId(1L);

        when(orderRepository.save(existingEntity)).thenReturn(existingEntity);

        var result = orderService.save(existingEntity);

        assertEquals(existingEntity, result);

        verify(orderRepository).save(existingEntity);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void deleteById() {
        Long id = 3L;
        orderService.deleteById(id);

        verify(orderRepository).deleteById(id);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder() {
        orderService.createOrder();
        // think or research about other way of testing or mocking a static method like localDateTime.now() other than using the ArgumentCaptor. (mockStatic)
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());
        verifyNoMoreInteractions(orderRepository);

        Order order = captor.getValue();

        assertFalse(order.getIsPaid());
        assertEquals(BigDecimal.ZERO, order.getTotalPrice());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void addItemToOrder_throwsWhenQuantityIsNullOrZero() {
        var exceptionWithZero = assertThrows(InvalidOrderRequestException.class, () -> orderService.addItemToOrder(1L, 10L, 0, null));
        var exceptionWithNull = assertThrows(InvalidOrderRequestException.class, () -> orderService.addItemToOrder(1L, 10L, null, null));

        String expectedMessage = "Quantity can not be null or less than 1";

        assertEquals(expectedMessage, exceptionWithZero.getMessage());
        assertEquals(expectedMessage, exceptionWithNull.getMessage());
    }

    @Test
    void addItemToOrder_throwsWhenMenuItemIsNotActive() {
        menuItem.setIsActive(false);

        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);

        var exception = assertThrows(InvalidOrderRequestException.class, () -> orderService.addItemToOrder(1L, 10L, 1, null));

        String expectedMessage = "Menu item not active: " + menuItem.getId();

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void addItemToOrder_throwsWhenMenuItemIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenThrow(new ResourceNotFoundException("MenuItem 10 not found"));

        var exception = assertThrows(ResourceNotFoundException.class, () -> orderService.addItemToOrder(1L, 10L, 1, null));

        String expectedMessage = "MenuItem " + menuItem.getId() + " not found";

        assertEquals(expectedMessage, exception.getMessage());

    }

    @Test
    void addItemToOrder_throwsWhenExtraIsNotFoundAndExtrasListsSizesMismatch() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);

        var exception = assertThrows(ResourceNotFoundException.class, () -> orderService.addItemToOrder(1L, 10L, 1, Set.of(5L)));

        String expectedMessage = "One or more extras were not found";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void addItemToOrder_throwsWhenExtraIsNotIsNotValidForItemOrNotActive() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);

        Extra activeExtra = new Extra();
        activeExtra.setId(1L);
        activeExtra.setIsActive(true);
        Extra notActiveExtra = new Extra();
        notActiveExtra.setId(2L);
        notActiveExtra.setIsActive(false);

        when(extraService.findAllById(Set.of(1L, 2L))).thenReturn(List.of(activeExtra, notActiveExtra));

        menuItem.setExtras(Set.of(
                activeExtra, notActiveExtra
        ));

        var exception = assertThrows(InvalidOrderRequestException.class, () -> orderService.addItemToOrder(1L, 10L, 1, Set.of(1L, 2L)));

        String expectedMessage = "Extra " + 2L + " is not valid for this item or is not active.";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void addItemToOrder_throwsWhenOrderIsNotFound() {
        var exception = assertThrows(ResourceNotFoundException.class, () -> orderService.addItemToOrder(1L, 10L, 1, null));

        String expectedMessage = "Order "+ 1L + " not found";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void addItemToOrder_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);

        var exception = assertThrows(OrderAlreadyPaidException.class, () -> orderService.addItemToOrder(1L, 10L, 1, null));

        String expectedMessage = "Order " + 1L + " is already paid!";

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void addItemToOrder_passesWhenAllGoodWithExtras() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);
        Extra extra = new Extra();
        extra.setIsActive(true);
        extra.setId(5L);
        when(extraService.findAllById(Set.of(5L))).thenReturn(List.of(extra));
        menuItem.setExtras(Set.of(extra));

        orderService.addItemToOrder(1L, 10L, 1, Set.of(5L));

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        assertFalse(order.getIsPaid());
        assertFalse(order.getOrderedItems().isEmpty());
        assertEquals(1L, order.getId());
    }

    @Test
    void addItemToOrder_throwsWhenExtraIsNotValidForThisItem() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);

        Extra extra = new Extra();
        extra.setId(5L);
        extra.setIsActive(true);

        menuItem.setExtras(new HashSet<>());

        when(extraService.findAllById(Set.of(5L)))
                .thenReturn(List.of(extra));

        var exception = assertThrows(
                InvalidOrderRequestException.class,
                () -> orderService.addItemToOrder(1L, 10L, 1, Set.of(5L))
        );

        assertEquals(
                "Extra 5 is not valid for this item or is not active.",
                exception.getMessage()
        );
    }

    @Test
    void addItemToOrder_passesWhenAllGoodWithoutExtras() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemService.findByIdOrThrow(10L)).thenReturn(menuItem);

        orderService.addItemToOrder(1L, 10L, 1, null);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        assertFalse(order.getIsPaid());
        assertFalse(order.getOrderedItems().isEmpty());
        assertEquals(1L, order.getId());
    }

    @Test
    void removeItemFromOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(2L);
        orderedItem.setMenuItem(menuItem);
        order.getOrderedItems().add(orderedItem);

        orderService.removeItemFromOrder(1L, 2L);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        assertTrue(order.getOrderedItems().isEmpty());
    }

    @Test
    void removeItemFromOrder_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));
        order.setIsPaid(true);

        var ex = assertThrows(OrderAlreadyPaidException.class, () -> orderService.removeItemFromOrder(1L, 10L));

        String expectedMessage = "Order " + order.getId() + " is already paid!";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeItemFromOrder_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.removeItemFromOrder(1L, 10L));

        String expectedMessage = "Order "+ order.getId() + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeItemFromOrder_throwsWhenItemIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.removeItemFromOrder(1L, 10L));

        String expectedMessage = "Ordered item " + 10L + " not found in this order";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void payOrder_throwsWhenEmptyOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.payOrder(1L, "example@gmail.com"));
        var expectedMessage = "Cannot pay for an empty order: " + order.getId();

        verifyNoInteractions(receiptService);

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void payOrder_throwsWhenEmailIsNullOrEmpty() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));
        order.getOrderedItems().add(new OrderedItem());

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.payOrder(1L, null));
        var expectedMessage = "Customer email is required to complete payment.";

        verifyNoInteractions(receiptService);

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void payOrder_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));
        order.setIsPaid(true);

        var ex = assertThrows(OrderAlreadyPaidException.class, () -> orderService.payOrder(1L, "example@gmail.com"));
        var expectedMessage = "Order " + order.getId() + " is already paid!";

        verifyNoInteractions(receiptService);

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void payOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.ofNullable(order));
        order.setIsPaid(false);
        order.getOrderedItems().add(new OrderedItem());

        orderService.payOrder(1L, "example@gmail.com");

        verify(orderRepository, times(1)).save(order);
        verify(receiptService, times(1)).sendReceipt(order);
        verifyNoMoreInteractions(orderRepository);

        assertTrue(order.getIsPaid());
    }

    @Test
    void payOrder_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.payOrder(1L, "example@gmail.com"));

        String expectedMessage = "Order "+ order.getId() + " not found";

        verifyNoInteractions(receiptService);

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void payOrder_throwsWhenEmailIsBlank() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        order.getOrderedItems().add(new OrderedItem());

        var ex = assertThrows(
                InvalidOrderRequestException.class,
                () -> orderService.payOrder(1L, "")
        );

        verifyNoInteractions(receiptService);

        assertEquals(
                "Customer email is required to complete payment.",
                ex.getMessage()
        );
    }

    @Test
    void updateQuantity_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.updateQuantity(1L, 10L, 5));

        String expectedMessage = "Order " + order.getId() + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void updateQuantity_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);

        var ex = assertThrows(OrderAlreadyPaidException.class, () -> orderService.updateQuantity(1L, 10L, 5));

        String expectedMessage = "Order " + order.getId() + " is already paid!";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void updateQuantity_throwsWhenOrderedItemIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.updateQuantity(1L, 10L, 5));

        String expectedMessage = "Ordered item " + 10L + " not found in this order";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void updateQuantity_throwsWhenQuantityIsNullOrZero() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setQuantity(2);
        order.getOrderedItems().add(orderedItem);

        var exceptionWithNull = assertThrows(InvalidOrderRequestException.class, () -> orderService.updateQuantity(1L, 10L, null));
        var exceptionWithZero = assertThrows(InvalidOrderRequestException.class, () -> orderService.updateQuantity(1L, 10L, 0));

        String expectedMessage = "Quantity can not be null or less than 1";

        assertEquals(expectedMessage, exceptionWithNull.getMessage());
        assertEquals(expectedMessage, exceptionWithZero.getMessage());
    }

    @Test
    void updateQuantity_treatsMissingPriceAndExtrasAsZero() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        var item = new OrderedItem();
        item.setId(10L);
        item.setQuantity(1);
        item.setPriceAtPurchase(null);
        item.setSelectedExtras(null);
        order.getOrderedItems().add(item);

        orderService.updateQuantity(1L, 10L, 2);

        assertEquals(BigDecimal.ZERO, order.getTotalPrice());
        verify(orderRepository).save(order);
    }

    @Test
    void updateQuantity_passesWhenAllGood() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setQuantity(1);
        orderedItem.setPriceAtPurchase(BigDecimal.valueOf(10));
        order.getOrderedItems().add(orderedItem);

        orderService.updateQuantity(1L, 10L, 3);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        var savedItem = order.getOrderedItems().stream().findFirst().orElseThrow();

        assertEquals(3, savedItem.getQuantity());
        assertEquals(BigDecimal.valueOf(30), order.getTotalPrice());
    }

    @Test
    void removeSelectedExtraOnOrderedItemFromOrder_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.removeSelectedExtraOnOrderedItemFromOrder(1L, 10L, 100L));

        String expectedMessage = "Order " + order.getId() + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeSelectedExtraOnOrderedItemFromOrder_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);

        var ex = assertThrows(OrderAlreadyPaidException.class, () -> orderService.removeSelectedExtraOnOrderedItemFromOrder(1L, 10L, 100L));

        String expectedMessage = "Order " + order.getId() + " is already paid!";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeSelectedExtraOnOrderedItemFromOrder_throwsWhenOrderedItemIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.removeSelectedExtraOnOrderedItemFromOrder(1L, 10L, 100L));

        String expectedMessage = "Ordered item " + 10L + " not found in this order";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeSelectedExtraOnOrderedItemFromOrder_throwsWhenSelectedExtraIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        order.getOrderedItems().add(orderedItem);

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.removeSelectedExtraOnOrderedItemFromOrder(1L, 10L, 100L));

        String expectedMessage = "Selected extra " + 100L + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void removeSelectedExtraOnOrderedItemFromOrder_passesWhenAllGood() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setQuantity(1);
        orderedItem.setPriceAtPurchase(BigDecimal.valueOf(10));

        var selectedExtra = new SelectedExtra();
        selectedExtra.setId(100L);
        selectedExtra.setPriceAtPurchase(BigDecimal.valueOf(2));
        orderedItem.setSelectedExtras(new HashSet<>(Set.of(selectedExtra)));

        order.getOrderedItems().add(orderedItem);

        orderService.removeSelectedExtraOnOrderedItemFromOrder(1L, 10L, 100L);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        var savedItem = order.getOrderedItems().stream().findFirst().orElseThrow();

        assertTrue(savedItem.getSelectedExtras().isEmpty());
        assertEquals(BigDecimal.valueOf(10), order.getTotalPrice());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Order " + order.getId() + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenOrderIsPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);

        var ex = assertThrows(OrderAlreadyPaidException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Order " + order.getId() + " is already paid!";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenOrderedItemIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Ordered item " + 10L + " not found in this order";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenExtraIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(extraService.findExtraByIdOrThrow(5L)).thenThrow(new ResourceNotFoundException("Extra " + 5L + " not found"));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setMenuItem(menuItem);
        order.getOrderedItems().add(orderedItem);

        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Extra " + 5L + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenExtraIsNotActive() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setMenuItem(menuItem);
        order.getOrderedItems().add(orderedItem);

        var extra = new Extra();
        extra.setId(5L);
        extra.setIsActive(false);
        when(extraService.findExtraByIdOrThrow(5L)).thenReturn(extra);

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Extra " + 5L + " is not active";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenExtraIsNotValidForThisItem() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setMenuItem(menuItem); // menuItem.getExtras() is empty
        order.getOrderedItems().add(orderedItem);

        var extra = new Extra();
        extra.setId(5L);
        extra.setIsActive(true);
        when(extraService.findExtraByIdOrThrow(5L)).thenReturn(extra);

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "You can not add this extra to this item";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_throwsWhenExtraIsAlreadyAdded() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var extra = new Extra();
        extra.setId(5L);
        extra.setIsActive(true);
        menuItem.setExtras(Set.of(extra));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setMenuItem(menuItem);

        var existingSelectedExtra = new SelectedExtra();
        existingSelectedExtra.setExtra(extra);
        orderedItem.setSelectedExtras(new HashSet<>(Set.of(existingSelectedExtra)));

        order.getOrderedItems().add(orderedItem);

        when(extraService.findExtraByIdOrThrow(5L)).thenReturn(extra);

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L));

        String expectedMessage = "Extra " + 5L + " is already added to this item";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void addExtraOnOrderedItemToOrder_passesWhenAllGood() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var extra = new Extra();
        extra.setId(5L);
        extra.setIsActive(true);
        extra.setPrice(BigDecimal.valueOf(2));
        menuItem.setExtras(Set.of(extra));

        var orderedItem = new OrderedItem();
        orderedItem.setId(10L);
        orderedItem.setMenuItem(menuItem);
        orderedItem.setQuantity(1);
        orderedItem.setPriceAtPurchase(BigDecimal.valueOf(10));

        order.getOrderedItems().add(orderedItem);

        when(extraService.findExtraByIdOrThrow(5L)).thenReturn(extra);

        orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        var savedItem = order.getOrderedItems().stream().findFirst().orElseThrow();

        assertEquals(1, savedItem.getSelectedExtras().size());
        assertEquals(BigDecimal.valueOf(12), order.getTotalPrice());
    }

    @Test
    void addExtraOnOrderedItemToOrder_allowsADifferentExistingExtra() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var requestedExtra = new Extra();
        requestedExtra.setId(5L);
        requestedExtra.setIsActive(true);
        requestedExtra.setPrice(BigDecimal.valueOf(2));

        var existingExtra = new Extra();
        existingExtra.setId(6L);
        existingExtra.setIsActive(true);
        existingExtra.setPrice(BigDecimal.ONE);

        menuItem.setExtras(Set.of(requestedExtra, existingExtra));
        var item = new OrderedItem();
        item.setId(10L);
        item.setMenuItem(menuItem);
        item.setQuantity(1);
        item.setPriceAtPurchase(BigDecimal.TEN);
        var selectedExistingExtra = new SelectedExtra();
        selectedExistingExtra.setExtra(existingExtra);
        selectedExistingExtra.setPriceAtPurchase(BigDecimal.ONE);
        item.setSelectedExtras(new HashSet<>(Set.of(selectedExistingExtra)));
        order.getOrderedItems().add(item);
        when(extraService.findExtraByIdOrThrow(5L)).thenReturn(requestedExtra);

        orderService.addExtraOnOrderedItemToOrder(1L, 10L, 5L);

        assertEquals(2, item.getSelectedExtras().size());
        assertEquals(BigDecimal.valueOf(13), order.getTotalPrice());
        verify(orderRepository).save(order);
    }

    @Test
    void calculateTotalPrice_returnsZeroWhenOrderItemsAreNull() throws Exception {
        var orderWithNoItems = new Order();
        orderWithNoItems.setOrderedItems(null);
        var method = OrderService.class.getDeclaredMethod("calculateTotalPrice", Order.class);
        method.setAccessible(true);

        assertEquals(BigDecimal.ZERO, method.invoke(orderService, orderWithNoItems));
    }

    @Test
    void calculateTotalPrice_treatsNullExtraPriceAsZero() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        var item = new OrderedItem();
        item.setId(10L);
        item.setQuantity(2);
        item.setPriceAtPurchase(BigDecimal.TEN);

        var selectedExtra = new SelectedExtra();
        selectedExtra.setPriceAtPurchase(null);

        item.setSelectedExtras(new HashSet<>(Set.of(selectedExtra)));

        order.getOrderedItems().add(item);

        orderService.updateQuantity(1L, 10L, 2);

        // (10 + 0) * 2
        assertEquals(BigDecimal.valueOf(20), order.getTotalPrice());

        verify(orderRepository).save(order);
    }

    @Test
    void findByIdOrThrow_withExistingOrder() {
        Long id = 1L;

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        var result = orderService.findByIdOrThrow(id);

        assertEquals(order, result);

        verify(orderRepository).findById(id);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findByIdOrThrow_withNonExistingOrder() {
        Long id = 1L;

        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.findByIdOrThrow(id)
        );

        assertEquals("Order " + id + " not found", exception.getMessage());

        verify(orderRepository).findById(id);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findAllPaidUnfulfilledOrders_mapsOrdersToKitchenTicketDto() {
        order.setIsPaid(true);
        order.setIsFulfilled(false);
        order.setCreatedAt(LocalDateTime.now());

        menuItem.setName("Cheeseburger");

        var extra = new Extra();
        extra.setName("Bacon");
        var selectedExtra = new SelectedExtra();
        selectedExtra.setExtra(extra);

        var orderedItem = new OrderedItem();
        orderedItem.setMenuItem(menuItem);
        orderedItem.setQuantity(2);
        orderedItem.setSelectedExtras(new HashSet<>(Set.of(selectedExtra)));

        order.getOrderedItems().add(orderedItem);

        when(orderRepository.findByIsPaidTrueAndIsFulfilledFalse()).thenReturn(List.of(order));

        var result = orderService.findAllPaidUnfulfilledOrders();

        assertEquals(1, result.size());

        var ticket = result.getFirst();
        assertEquals(order.getId(), ticket.getOrderId());
        assertEquals(order.getCreatedAt(), ticket.getCreatedAt());
        assertEquals(1, ticket.getItems().size());

        var itemDto = ticket.getItems().getFirst();
        assertEquals("Cheeseburger", itemDto.getItemName());
        assertEquals(2, itemDto.getQuantity());
        assertEquals(List.of("Bacon"), itemDto.getExtraNames());

        verify(orderRepository).findByIsPaidTrueAndIsFulfilledFalse();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findAllPaidUnfulfilledOrders_sortsOldestFirst() {
        var olderOrder = new Order(1L, LocalDateTime.now().minusDays(2), BigDecimal.ZERO, true, false, "", new HashSet<>(), null);
        var newerOrder = new Order(2L, LocalDateTime.now(), BigDecimal.ZERO, true, false, "", new HashSet<>(), null);

        when(orderRepository.findByIsPaidTrueAndIsFulfilledFalse()).thenReturn(List.of(newerOrder, olderOrder));

        var result = orderService.findAllPaidUnfulfilledOrders();

        assertEquals(2, result.size());
        assertEquals(olderOrder.getId(), result.get(0).getOrderId());
        assertEquals(newerOrder.getId(), result.get(1).getOrderId());

        verify(orderRepository).findByIsPaidTrueAndIsFulfilledFalse();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void markFulfilled_throwsWhenOrderIsNotFound() {
        var ex = assertThrows(ResourceNotFoundException.class, () -> orderService.markFulfilled(1L));

        String expectedMessage = "Order " + 1L + " not found";

        assertEquals(expectedMessage, ex.getMessage());
    }

    @Test
    void markFulfilled_throwsWhenOrderIsNotPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(false);

        var ex = assertThrows(InvalidOrderRequestException.class, () -> orderService.markFulfilled(1L));

        String expectedMessage = "Order " + order.getId() + " is not paid yet.";

        assertEquals(expectedMessage, ex.getMessage());

        verify(orderRepository).findById(1L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void markFulfilled_setsFulfilledWhenOrderIsPaidAndNotYetFulfilled() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);
        order.setIsFulfilled(false);

        orderService.markFulfilled(1L);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        assertTrue(order.getIsFulfilled());
    }

    @Test
    void markFulfilled_isIdempotent_whenOrderIsAlreadyFulfilled() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        order.setIsPaid(true);
        order.setIsFulfilled(true);

        orderService.markFulfilled(1L);

        verify(orderRepository, times(1)).save(order);
        verifyNoMoreInteractions(orderRepository);

        assertTrue(order.getIsFulfilled());
    }
}