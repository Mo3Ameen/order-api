package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.*;
import io.everyonecodes.order_api.repository.OrderedItemRepository;
import io.everyonecodes.order_api.repository.SelectedExtraRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectedExtraServiceTest {

    @InjectMocks
    private SelectedExtraService service;

    @Mock
    private SelectedExtraRepository repository;

    @Test
    void findAll() {
        List<SelectedExtra> selectedExtras = List.of(
                new SelectedExtra(1L, BigDecimal.valueOf(2), new Extra(), new OrderedItem()),
                new SelectedExtra(2L, BigDecimal.valueOf(3), new Extra(), new OrderedItem()),
                new SelectedExtra(3L, BigDecimal.valueOf(1.5), new Extra(), new OrderedItem())
        );

        when(repository.findAll()).thenReturn(selectedExtras);

        var result = service.findAll();

        List<SelectedExtra> expected = List.of(
                new SelectedExtra(1L, BigDecimal.valueOf(2), new Extra(), new OrderedItem()),
                new SelectedExtra(2L, BigDecimal.valueOf(3), new Extra(), new OrderedItem()),
                new SelectedExtra(3L, BigDecimal.valueOf(1.5), new Extra(), new OrderedItem())
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withExistingSelectedExtra() {
        Long id = 1L;
        var expected = new SelectedExtra(1L, BigDecimal.valueOf(2), new Extra(), new OrderedItem());

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withNonExistingSelectedExtra() {
        Long id = 4L;
        Optional<SelectedExtra> expected = Optional.empty();

        when(repository.findById(id)).thenReturn(expected);

        var result = service.findById(id);

        assertFalse(result.isPresent());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_newSelectedExtra() {
        var newEntity = new SelectedExtra();
        var expected = new SelectedExtra();

        when(repository.save(newEntity)).thenReturn(expected);

        var result = service.save(newEntity);

        assertEquals(expected, result);

        verify(repository).save(newEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_existingSelectedExtra() {
        var existingEntity = new SelectedExtra();
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