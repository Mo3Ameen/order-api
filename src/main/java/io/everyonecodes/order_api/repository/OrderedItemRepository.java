package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.OrderedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderedItemRepository extends JpaRepository<OrderedItem, Long> {
}