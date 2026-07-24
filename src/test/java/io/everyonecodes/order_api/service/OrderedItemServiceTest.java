package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.entity.Order;
import io.everyonecodes.order_api.entity.OrderedItem;
import io.everyonecodes.order_api.repository.OrderedItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderedItemServiceTest {

    @InjectMocks
    private OrderedItemService service;

    @Mock
    private OrderedItemRepository repository;

    @Test
    void findAll() {
        List<OrderedItem> orderedItems = List.of(
                new OrderedItem(1L, 1, BigDecimal.valueOf(10), new MenuItem(), new Order(), new HashSet<>()),
                new OrderedItem(2L, 2, BigDecimal.valueOf(5), new MenuItem(), new Order(), new HashSet<>()),
                new OrderedItem(3L, 3, BigDecimal.valueOf(25.5), new MenuItem(), new Order(), new HashSet<>())
        );

        when(repository.findAll()).thenReturn(orderedItems);

        var result = service.findAll();

        List<OrderedItem> expected = List.of(
                new OrderedItem(1L, 1, BigDecimal.valueOf(10), new MenuItem(), new Order(), new HashSet<>()),
                new OrderedItem(2L, 2, BigDecimal.valueOf(5), new MenuItem(), new Order(), new HashSet<>()),
                new OrderedItem(3L, 3, BigDecimal.valueOf(25.5), new MenuItem(), new Order(), new HashSet<>())
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withExistingOrderedItem() {
        Long id = 1L;
        var expected = new OrderedItem(1L, 1, BigDecimal.valueOf(10), new MenuItem(), new Order(), new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withNonExistingOrderedItem() {
        Long id = 4L;
        Optional<OrderedItem> expected = Optional.empty();

        when(repository.findById(id)).thenReturn(expected);

        var result = service.findById(id);

        assertFalse(result.isPresent());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_newOrderedItem() {
        var newEntity = new OrderedItem();
        var expected = new OrderedItem();

        when(repository.save(newEntity)).thenReturn(expected);

        var result = service.save(newEntity);

        assertEquals(expected, result);

        verify(repository).save(newEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_existingOrderedItem() {
        var existingEntity = new OrderedItem();
        existingEntity.setId(1L);

        when(repository.save(existingEntity)).thenReturn(existingEntity);

        var result = service.save(existingEntity);

        assertEquals(existingEntity, result);

        verify(repository).save(existingEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void deleteById() {
        Long id = 3L;
        service.deleteById(id);

        verify(repository).deleteById(id);
        verifyNoMoreInteractions(repository);
    }
}