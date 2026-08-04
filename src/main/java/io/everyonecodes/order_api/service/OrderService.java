package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.*;
import io.everyonecodes.order_api.exception.InvalidOrderRequestException;
import io.everyonecodes.order_api.exception.OrderAlreadyPaidException;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final ExtraService extraService;

    public OrderService(OrderRepository orderRepository, MenuItemService menuItemService, ExtraService extraService) {
        this.orderRepository = orderRepository;
        this.menuItemService = menuItemService;
        this.extraService = extraService;
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public void deleteById(Long id) {
        orderRepository.deleteById(id);
    }

    public Order createOrder() {
        Order order = new Order();
        order.setCreatedAt(LocalDateTime.now());
        order.setIsPaid(false);
        order.setTotalPrice(BigDecimal.ZERO);
        return orderRepository.save(order);
    }

    @Transactional
    public Order addItemToOrder(Long orderId, Long menuItemId, Integer quantity, Set<Long> extraIds) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidOrderRequestException("Quantity can not be null or less than 1");
        }

        Order order = getValidOrder(orderId);

        MenuItem menuItem = menuItemService.findById(menuItemId).orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));

        if (!menuItem.getIsActive()) {
            throw new InvalidOrderRequestException("Menu item not active: " + menuItemId);
        }

        // Initialize OrderedItem
        OrderedItem orderedItem = new OrderedItem();
        orderedItem.setQuantity(quantity);
        orderedItem.setOrder(order);
        orderedItem.setMenuItem(menuItem);
        orderedItem.setPriceAtPurchase(menuItem.getPrice());

        // Normalize extraIds to avoid NullPointerExceptions later
        if (extraIds == null) {
            extraIds = Collections.emptySet();
        }

        if (!extraIds.isEmpty()) {
            // Fetch all extras in ONE query (Better performance)
            List<Extra> foundedExtras = extraService.findAllById(extraIds);
            if (foundedExtras.size() != extraIds.size()) {
                throw new ResourceNotFoundException("One or more extras were not found");
            }

            Set<SelectedExtra> selectedExtras = orderedItem.getSelectedExtras();

            for (Extra extra : foundedExtras) {
                if (menuItem.getExtras().contains(extra) && extra.getIsActive()) {
                    SelectedExtra selectedExtra = new SelectedExtra();
                    selectedExtra.setOrderedItem(orderedItem);
                    selectedExtra.setExtra(extra);
                    selectedExtra.setPriceAtPurchase(extra.getPrice());
                    selectedExtras.add(selectedExtra);
                } else {
                    throw new InvalidOrderRequestException("Extra " + extra.getId() + " is not valid for this item or is not active.");
                }
            }
        }

        order.getOrderedItems().add(orderedItem);
        order.setTotalPrice(calculateTotalPrice(order));

        return orderRepository.save(order);
    }

    @Transactional
    public Order removeItemFromOrder(Long orderId, Long orderedItemId) {
        Order order = getValidOrder(orderId);

        OrderedItem orderedItem = getOrderedItemFromOrder(order, orderedItemId);

        order.getOrderedItems().remove(orderedItem);
        order.setTotalPrice(calculateTotalPrice(order));
        return orderRepository.save(order);
    }

    @Transactional
    public Order payOrder(Long orderId, String email) {
        Order order = getValidOrder(orderId);

        if (order.getOrderedItems().isEmpty()) {
            throw new InvalidOrderRequestException("Cannot pay for an empty order: " + orderId);
        } else if (email == null || email.isBlank()) {
            throw new InvalidOrderRequestException("Customer email is required to complete payment.");
        } else {
            order.setCustomerEmail(email);
            order.setIsPaid(true);
            return orderRepository.save(order);
        }
    }

    @Transactional
    public Order updateQuantity(Long orderId, Long orderedItemId, Integer quantity) {
        Order order = getValidOrder(orderId);

        OrderedItem orderedItem = getOrderedItemFromOrder(order, orderedItemId);

        if (quantity != null && quantity > 0) {
            orderedItem.setQuantity(quantity);
        } else {
            throw new InvalidOrderRequestException("Quantity can not be null or less than 1");
        }

        order.setTotalPrice(calculateTotalPrice(order));

        return orderRepository.save(order);
    }

    @Transactional
    public Order removeSelectedExtraOnOrderedItemFromOrder(Long orderId, Long orderedItemId, Long selectedExtraId) {
        Order order = getValidOrder(orderId);

        OrderedItem item = getOrderedItemFromOrder(order, orderedItemId);

        SelectedExtra extra = item.getSelectedExtras().stream()
                .filter(e -> e.getId().equals(selectedExtraId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Selected extra " + selectedExtraId + " not found")
                );

        item.getSelectedExtras().remove(extra);
        order.setTotalPrice(calculateTotalPrice(order));

        return orderRepository.save(order);
    }

    @Transactional
    public Order addExtraOnOrderedItemToOrder(Long orderId, Long orderedItemId, Long extraId) {
        Order order = getValidOrder(orderId);

        OrderedItem item = getOrderedItemFromOrder(order, orderedItemId);

        Extra extra = extraService.findById(extraId).orElseThrow(() -> new ResourceNotFoundException("Extra "+ extraId + " not found"));

        if (!extra.getIsActive()) {
            throw new InvalidOrderRequestException("Extra " + extraId + " is not active");
        }

        if (!item.getMenuItem().getExtras().contains(extra)) {
            throw new InvalidOrderRequestException("You can not add this extra to this item");
        }

        Set<SelectedExtra> selectedExtras = item.getSelectedExtras();

        for (SelectedExtra selectedExtra : selectedExtras) {
            if (selectedExtra.getExtra().getId().equals(extraId)) {
                throw new InvalidOrderRequestException("Extra " + extraId + " is already added to this item");
            }
        }

        SelectedExtra selectedExtra = new SelectedExtra();
        selectedExtra.setExtra(extra);
        selectedExtra.setPriceAtPurchase(extra.getPrice());
        selectedExtra.setOrderedItem(item);
        item.getSelectedExtras().add(selectedExtra);
        order.setTotalPrice(calculateTotalPrice(order));

        return orderRepository.save(order);
    }

    public Order findByIdOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order "+ id + " not found"));
    }

    private BigDecimal calculateTotalPrice(Order order) {
        if (order.getOrderedItems() == null) {
            return BigDecimal.ZERO;
        }

        return order.getOrderedItems().stream()
                .map(item -> {
                    BigDecimal extrasTotal = item.getSelectedExtras() == null ? BigDecimal.ZERO : item.getSelectedExtras().stream()
                            .map(SelectedExtra::getPriceAtPurchase)
                            .map(price -> price == null ? BigDecimal.ZERO : price)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal itemPrice = item.getPriceAtPurchase() == null ? BigDecimal.ZERO : item.getPriceAtPurchase();

                    return extrasTotal.add(itemPrice).multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static OrderedItem getOrderedItemFromOrder(Order order, Long orderedItemId) {
        return order.getOrderedItems().stream()
                .filter(e -> e.getId().equals(orderedItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ordered item " + orderedItemId + " not found in this order")
                );
    }

    private Order getValidOrder(Long orderId) {
        Order order = findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order "+ orderId + " not found"));

        if (order.getIsPaid()) {
            throw new OrderAlreadyPaidException("Order " + orderId + " is already paid!");
        }

        return order;
    }
}
