package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Extra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtraRepository extends JpaRepository<Extra, Long> {
}
