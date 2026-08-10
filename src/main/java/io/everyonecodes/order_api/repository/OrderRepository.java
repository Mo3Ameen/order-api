package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsPaidTrueAndIsFulfilledFalse();
}