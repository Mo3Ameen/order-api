package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks // This replaces the setUp() method.
    private CategoryService service;

    @Mock
    private CategoryRepository repository;

    // If there were no @InjectMocks annotation then use this.
//    @BeforeEach
//    void setUp() {
//        service = new CategoryService(repository);
//    }

    private final List<Category> allCategories = List.of(
            new Category(1L, "Pizzas", true, new HashSet<>()),
            new Category(2L, "Deserts", false, new HashSet<>()),
            new Category(3L, "Drinks", true, new HashSet<>())
    );

    @Test
    void findAll() {
        when(repository.findAll()).thenReturn(allCategories);

        var result = service.findAll();

        assertEquals(allCategories, result);

        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findAllActive() {
        var expected = List.of(
                new Category(1L, "Pizzas", true, new HashSet<>()),
                new Category(3L, "Drinks", true, new HashSet<>())
                );

        when(repository.findByIsActive(true)).thenReturn(expected);

        var result = service.findAllActive();

        assertEquals(expected, result);

        verify(repository).findByIsActive(true);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withExistingCategory() {
        Long id = 1L;
        var expected = new Category(id, "Pizzas", true, new HashSet<>());

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findById(id);

        assertTrue(result.isPresent());
        assertEquals(expected.getId(), result.get().getId());
        assertEquals(expected.getName(), result.get().getName());
        assertEquals(expected.getIsActive(), result.get().getIsActive());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findById_withNonExistingCategory() {
        Long id = 4L;
        Optional<Category> expected = Optional.empty();

        when(repository.findById(id)).thenReturn(expected);

        var result = service.findById(id);

        assertFalse(result.isPresent());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_newCategory() {
        var newEntity = new Category();
        var expected = new Category();

        when(repository.save(newEntity)).thenReturn(expected);

        var result = service.save(newEntity);

        assertEquals(expected.getId(), result.getId());

        verify(repository).save(newEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void save_existingCategory() {
        var existingEntity = new Category();
        existingEntity.setId(1L);

        when(repository.save(existingEntity)).thenReturn(existingEntity);

        var result = service.save(existingEntity);

        assertEquals(existingEntity.getId(), result.getId());

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