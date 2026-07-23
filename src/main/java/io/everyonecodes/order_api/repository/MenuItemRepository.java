package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCategoryAndIsActive(Category category, Boolean isActive);
}