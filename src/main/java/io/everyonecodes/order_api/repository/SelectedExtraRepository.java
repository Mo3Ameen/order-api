package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.SelectedExtra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelectedExtraRepository extends JpaRepository<SelectedExtra, Long> {
}