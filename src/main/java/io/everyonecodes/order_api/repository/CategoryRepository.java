package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByIsActive(Boolean isActive);
    Optional<Category> findByIdAndIsActiveTrue(Long id);
}