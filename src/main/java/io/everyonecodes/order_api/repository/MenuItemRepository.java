package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCategoryAndIsActive(Category category, Boolean isActive);
    Optional<MenuItem> findByIdAndIsActiveTrue(Long id);
}