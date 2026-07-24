package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.repository.MenuItemRepository;
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
class MenuItemServiceTest {

    @InjectMocks
    private MenuItemService service;

    @Mock
    private MenuItemRepository repository;

    @Test
    void findAll() {
        List<MenuItem>  menuItems = List.of(
                new MenuItem(1L, "Pizza", "", BigDecimal.valueOf(10), "", true, new HashSet<>(), null),
                new MenuItem(2L, "Burger", "", BigDecimal.valueOf(12), "", false, new HashSet<>(), null),
                new MenuItem(3L, "Cola", "", BigDecimal.valueOf(2.5), "", true, new HashSet<>(), null)
        );

        when(repository.findAll()).thenReturn(menuItems);

        var result = service.findAll();

        List<MenuItem> expected = List.of(
                new MenuItem(1L, "Pizza", "", BigDecimal.valueOf(10), "", true, new HashSet<>(), null),
                new MenuItem(2L, "Burger", "", BigDecimal.valueOf(12), "", false, new HashSet<>(), null),
                new MenuItem(3L, "Cola", "", BigDecimal.valueOf(2.5), "", true, new HashSet<>(), null)
        );

        assertEquals(expected, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withExistingMenuItem() {
        Long id = 1L;
        var expected = new MenuItem(1L, "Pizza", "", BigDecimal.valueOf(10), "", true, new HashSet<>(), null);

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withNonExistingMenuItem() {
        Long id = 4L;
        Optional<MenuItem> expected = Optional.empty();

        when(repository.findById(id)).thenReturn(expected);

        var result = service.findById(id);

        assertFalse(result.isPresent());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByCategoryAndIsActive() {
        var category = new Category();
        category.setId(1L);
        category.setIsActive(true);
        category.setName("Drinks");

        var menuItem = new MenuItem();
        menuItem.setIsActive(true);
        menuItem.setName("Cola");
        menuItem.setId(3L);
        menuItem.setCategory(category);

        when(repository.findByCategoryAndIsActive(category, true)).thenReturn(List.of(menuItem));

        var result = service.findByCategoryAndIsActive(category);

        assertEquals(menuItem, result.getFirst());

        verify(repository).findByCategoryAndIsActive(category, true);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_newMenuItem() {
        var newEntity = new MenuItem();
        var expected = new MenuItem();

        when(repository.save(newEntity)).thenReturn(expected);

        var result = service.save(newEntity);

        assertEquals(expected, result);

        verify(repository).save(newEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_existingMenuItem() {
        var existingEntity = new MenuItem();
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