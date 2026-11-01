package io.everyonecodes.order_api.repository;

import io.everyonecodes.order_api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByIsPaidTrueAndIsFulfilledFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Order o set o.isPaid = true, o.customerEmail = :email, o.paidAt = :paidAt where o.id = :id and o.isPaid = false")
    int markAsPaid(@Param("id") Long id, @Param("email") String email,@Param("paidAt") LocalDateTime paidAt);
}