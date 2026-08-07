package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
import io.everyonecodes.order_api.repository.ExtraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;

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

    @Test
    void createExtra_resetsIdAndDefaultsToActive() {
        var extra = new Extra(5L, "Olives", BigDecimal.ONE, null, new HashSet<>());
        when(repository.save(extra)).thenReturn(extra);

        var result = service.createExtra(extra);

        assertNull(result.getId());
        assertTrue(result.getIsActive());
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void createExtra_preservesAnExplicitInactiveFlag() {
        var extra = new Extra(5L, "Archived Olives", BigDecimal.ONE, false, new HashSet<>());
        when(repository.save(extra)).thenReturn(extra);

        var result = service.createExtra(extra);

        assertNull(result.getId());
        assertFalse(result.getIsActive());
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void updateExtra_updatesEveryMutableField() {
        var existing = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        var update = new Extra(null, "Bacon", BigDecimal.valueOf(2), false, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateExtra(update, 1L);

        assertEquals("Bacon", result.getName());
        assertEquals(BigDecimal.valueOf(2), result.getPrice());
        assertFalse(result.getIsActive());
        verify(repository).findById(1L);
        verify(repository).save(existing);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void softDeleteById_marksExtraInactive() {
        var extra = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(extra));

        service.softDeleteById(1L);

        assertFalse(extra.getIsActive());
        verify(repository).findById(1L);
        verify(repository).save(extra);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findExtraByIdOrThrow_returnsExtraOrThrows() {
        var extra = new Extra(1L, "Cheese", BigDecimal.ONE, true, new HashSet<>());
        when(repository.findById(1L)).thenReturn(Optional.of(extra));
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertEquals(extra, service.findExtraByIdOrThrow(1L));
        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findExtraByIdOrThrow(2L));
        assertEquals("Extra 2 not found", exception.getMessage());

        verify(repository).findById(1L);
        verify(repository).findById(2L);
        verifyNoMoreInteractions(repository);
    }
}