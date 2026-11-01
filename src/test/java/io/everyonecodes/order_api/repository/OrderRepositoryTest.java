package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void markAsPaid_changesAnUnpaidOrderOnlyOnce() {
        var order = new Order();
        order.setCreatedAt(LocalDateTime.now());
        order.setIsPaid(false);
        order.setIsFulfilled(false);
        order.setTotalPrice(BigDecimal.TEN);
        order = orderRepository.save(order);

        var first = orderRepository.markAsPaid(order.getId(), "first@example.com", LocalDateTime.now());
        var second = orderRepository.markAsPaid(order.getId(), "second@example.com", LocalDateTime.now());

        assertEquals(1, first);
        assertEquals(0, second);

        var reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertTrue(reloaded.getIsPaid());
        assertEquals("first@example.com", reloaded.getCustomerEmail());
        assertNotNull(reloaded.getPaidAt());
    }
}