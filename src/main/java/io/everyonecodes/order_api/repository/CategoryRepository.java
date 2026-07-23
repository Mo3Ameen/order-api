package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActive(Boolean isActive);
}