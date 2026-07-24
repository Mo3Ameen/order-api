package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.ExtraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtraServiceTest {

    @InjectMocks
    private ExtraService service;

    @Mock
    private ExtraRepository repository;

    @Test
    void findAll() {
        List<Extra> extras = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(2L, "Bacon", BigDecimal.valueOf(2), false, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAll()).thenReturn(extras);

        var result = service.findAll();

        List<Extra> expected = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(2L, "Bacon", BigDecimal.valueOf(2), false, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByMenuItemsContainingAndIsActive() {
        var item = new MenuItem();
        item.setId(1L);
        item.setName("Burger");
        item.setIsActive(true);

        var extra = new Extra();
        extra.setId(10L);
        extra.setName("Extra Cheese");
        extra.setIsActive(true);

        when(repository.findByMenuItemsContainingAndIsActive(item, true)).thenReturn(List.of(extra));

        var result = service.findByMenuItemsContainingAndIsActive(item);

        assertEquals(extra, result.getFirst());

        verify(repository).findByMenuItemsContainingAndIsActive(item, true);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withExistingExtra() {
        Long id = 1L;
        var expected = new Extra(1L, "Extra Cheese", BigDecimal.valueOf(2), true, new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withNonExistingExtra() {
        Long id = 4L;
        Optional<Extra> expected = Optional.empty();

        when(repository.findById(id)).thenReturn(expected);

        var result = service.findById(id);

        assertFalse(result.isPresent());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_newExtra() {
        var newEntity = new Extra();
        var expected = new Extra();

        when(repository.save(newEntity)).thenReturn(expected);

        var result = service.save(newEntity);

        assertEquals(expected, result);

        verify(repository).save(newEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_existingExtra() {
        var existingEntity = new Extra();
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

    @Test
    void findAllById_passingCase() {
        Set<Long> ids = Set.of(1L, 3L);

        var expected = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAllById(ids)).thenReturn(expected);

        var result = service.findAllById(ids);

        assertEquals(expected, result);

        verify(repository).findAllById(ids);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findAllById_returnsFewerExtras_whenSomeIdsDontExist() {
        Set<Long> ids = Set.of(1L, 2L, 3L);

        var founded = List.of(
                new Extra(1L, "Extra Cheese", BigDecimal.valueOf(1.5), true, new HashSet<>()),
                new Extra(3L, "Mushrooms", BigDecimal.valueOf(1), true, new HashSet<>())
        );

        when(repository.findAllById(ids)).thenReturn(founded);

        var result = service.findAllById(ids);

        assertEquals(2, result.size());
        assertNotEquals(ids.size(), result.size());

        verify(repository).findAllById(ids);
        verifyNoMoreInteractions(repository);
    }
}