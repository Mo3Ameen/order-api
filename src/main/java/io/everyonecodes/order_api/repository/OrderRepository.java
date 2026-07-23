package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}