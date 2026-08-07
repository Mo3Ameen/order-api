package io.everyonecodes.order_api.service;

import io.everyonecodes.order_api.dto.MenuItemRequestDto;
import io.everyonecodes.order_api.entity.Category;
import io.everyonecodes.order_api.entity.Extra;
import io.everyonecodes.order_api.entity.MenuItem;
import io.everyonecodes.order_api.exception.ResourceNotFoundException;
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
    @Mock
    private CategoryService categoryService;
    @Mock
    private ExtraService extraService;

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

        when(categoryService.getByIdAndIsActiveOrThrow(1L)).thenReturn(category);
        when(repository.findByCategoryAndIsActive(category, true)).thenReturn(List.of(menuItem));

        var result = service.findByCategoryAndIsActive(1L);

        assertEquals(menuItem, result.getFirst());

        verify(categoryService).getByIdAndIsActiveOrThrow(1L);
        verify(repository).findByCategoryAndIsActive(category, true);
        verifyNoMoreInteractions(repository, categoryService);
    }

    @Test
    void findByIdAndIsActiveOrThrow_withExistingMenuItem() {
        Long id = 1L;
        var expected = new MenuItem(1L, "Pizza", "", BigDecimal.valueOf(10), "", true, new HashSet<>(), null);

        when(repository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(expected));

        var result = service.findByIdAndIsActiveOrThrow(id);

        assertEquals(expected, result);

        verify(repository).findByIdAndIsActiveTrue(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByIdAndIsActiveOrThrow_withNonExistingMenuItem_throws() {
        Long id = 4L;

        when(repository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findByIdAndIsActiveOrThrow(id));

        assertEquals("MenuItem " + id + " not found", exception.getMessage());

        verify(repository).findByIdAndIsActiveTrue(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getExtras() {
        Long id = 1L;
        var menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setIsActive(true);

        var extra = new Extra();
        extra.setId(10L);
        extra.setName("Extra Cheese");

        when(repository.findByIdAndIsActiveTrue(id)).thenReturn(Optional.of(menuItem));
        when(extraService.findByMenuItemsContainingAndIsActive(menuItem)).thenReturn(List.of(extra));

        var result = service.getExtras(id);

        assertEquals(1, result.size());
        assertEquals(extra, result.getFirst());

        verify(repository).findByIdAndIsActiveTrue(id);
        verify(extraService).findByMenuItemsContainingAndIsActive(menuItem);
        verifyNoMoreInteractions(repository, extraService);
    }

    @Test
    void findByIdOrThrow_withExistingMenuItem() {
        Long id = 1L;
        var expected = new MenuItem();
        expected.setId(id);

        when(repository.findById(id)).thenReturn(Optional.of(expected));

        var result = service.findByIdOrThrow(id);

        assertEquals(expected, result);

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findByIdOrThrow_withNonExistingMenuItem_throws() {
        Long id = 4L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ResourceNotFoundException.class, () -> service.findByIdOrThrow(id));

        assertEquals("MenuItem " + id + " not found", exception.getMessage());

        verify(repository).findById(id);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void softDeleteMenuItemById() {
        Long id = 1L;
        var menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setIsActive(true);

        when(repository.findById(id)).thenReturn(Optional.of(menuItem));
        when(repository.save(menuItem)).thenReturn(menuItem);

        service.softDeleteMenuItemById(id);

        assertFalse(menuItem.getIsActive());

        verify(repository).findById(id);
        verify(repository).save(menuItem);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void postMenuItem() {
        var dto = new MenuItemRequestDto();
        dto.setCategoryId(1L);
        dto.setName("Burger");
        dto.setPrice(BigDecimal.valueOf(10));
        dto.setIsActive(null);

        var category = new Category();
        category.setId(1L);

        when(categoryService.findByIdOrThrow(1L)).thenReturn(category);

        when(repository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        var result = service.postMenuItem(dto);

        assertEquals("Burger", result.getName());
        assertEquals(category, result.getCategory());
        assertTrue(result.getIsActive());

        verify(categoryService).findByIdOrThrow(1L);
        verify(repository).save(any(MenuItem.class));
        verifyNoMoreInteractions(categoryService, repository);
    }

    @Test
    void postMenuItem_preservesAnExplicitInactiveFlag() {
        var dto = new MenuItemRequestDto("Burger", "", BigDecimal.TEN, "", false, 1L);
        var category = new Category();
        when(categoryService.findByIdOrThrow(1L)).thenReturn(category);
        when(repository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.postMenuItem(dto);

        assertFalse(result.getIsActive());
        verify(categoryService).findByIdOrThrow(1L);
        verify(repository).save(any(MenuItem.class));
        verifyNoMoreInteractions(categoryService, repository);
    }

    @Test
    void putMenuItem() {
        Long id = 1L;

        var dto = new MenuItemRequestDto();
        dto.setCategoryId(2L);
        dto.setName("Updated Burger");
        dto.setIsActive(false);

        var existingItem = new MenuItem();
        existingItem.setId(id);
        existingItem.setName("Old Burger");

        var newCategory = new Category();
        newCategory.setId(2L);

        when(repository.findById(id)).thenReturn(Optional.of(existingItem));
        when(categoryService.findByIdOrThrow(2L)).thenReturn(newCategory);
        when(repository.save(existingItem)).thenReturn(existingItem);

        var result = service.putMenuItem(dto, id);

        assertEquals("Updated Burger", result.getName());
        assertEquals(newCategory, result.getCategory());
        assertFalse(result.getIsActive());

        verify(repository).findById(id);
        verify(categoryService).findByIdOrThrow(2L);
        verify(repository).save(existingItem);
        verifyNoMoreInteractions(repository, categoryService);
    }
}