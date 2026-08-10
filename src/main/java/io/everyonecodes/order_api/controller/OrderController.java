package io.everyonecodes.order_api.controller;

import io.everyonecodes.order_api.dto.AddItemRequestDto;
import io.everyonecodes.order_api.dto.KitchenTicketDto;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderService.findByIdOrThrow(orderId);
    }

    @GetMapping("/kitchen")
    public List<KitchenTicketDto> findAllPaidUnfulfilled() {
        return orderService.findAllPaidUnfulfilledOrders();
    }

    @PutMapping("/{orderId}/fulfill")
    public Order markFulfilled(@PathVariable Long orderId) {
        return orderService.markFulfilled(orderId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order postOrder() {
        return orderService.createOrder();
    }

    @PostMapping("/{orderId}/items")
    public Order addItemToOrder(@PathVariable Long orderId, @RequestBody @Valid AddItemRequestDto dto) {
        return orderService.addItemToOrder(orderId, dto.getMenuItemId(), dto.getQuantity(), dto.getExtraIds());
    }

    @PutMapping("/{orderId}/items/{itemId}")
    public Order updateQuantity(@PathVariable Long orderId, @PathVariable Long itemId, @RequestParam Integer quantity) {
        return orderService.updateQuantity(orderId, itemId, quantity);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public Order removeItemFromOrder(@PathVariable Long orderId, @PathVariable Long itemId) {
        return orderService.removeItemFromOrder(orderId, itemId);
    }

    @PostMapping("/{orderId}/items/{itemId}/extras/{extraId}")
    public Order addExtra(@PathVariable Long orderId, @PathVariable Long itemId, @PathVariable Long extraId) {
        return orderService.addExtraOnOrderedItemToOrder(orderId, itemId, extraId);
    }

    @DeleteMapping("/{orderId}/items/{itemId}/extras/{selectedExtraId}")
    public Order removeExtra(@PathVariable Long orderId, @PathVariable Long itemId, @PathVariable Long selectedExtraId) {
        return orderService.removeSelectedExtraOnOrderedItemFromOrder(orderId, itemId, selectedExtraId);
    }

    @PostMapping("/{orderId}/pay")
    public Order payOrder(@PathVariable Long orderId, @RequestParam @Email String email) {
        return orderService.payOrder(orderId, email);
    }
}