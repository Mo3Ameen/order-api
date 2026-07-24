package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface ExtraRepository extends JpaRepository<Extra, Long> {
    List<Extra> findByMenuItemsContainingAndIsActive(MenuItem menuItem, Boolean isActive);
}
